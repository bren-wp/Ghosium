#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <shellapi.h>
#include <shlobj.h>

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <cwctype>
#include <filesystem>
#include <fstream>
#include <string>
#include <system_error>
#include <thread>
#include <vector>

namespace fs = std::filesystem;

namespace {

constexpr wchar_t kProductName[] = L"Ghosium Browser";
constexpr wchar_t kCompanyDirectory[] = L"Brendigo";
constexpr wchar_t kProfileDirectory[] = L"Ghosium";
constexpr wchar_t kEngineExecutable[] = L"firefox.exe";
constexpr wchar_t kTorExecutable[] = L"Tor\\tor.exe";
constexpr wchar_t kSelfTestSwitch[] = L"--ghosium-self-test";
constexpr wchar_t kWaitSwitch[] = L"--ghosium-wait";
constexpr wchar_t kPortableProfilePrefix[] = L"--ghosium-portable-profile=";
constexpr wchar_t kLanguagePrefix[] = L"--ghosium-language=";

std::wstring ToLower(std::wstring value) {
  std::transform(value.begin(), value.end(), value.begin(), [](wchar_t ch) {
    return static_cast<wchar_t>(std::towlower(ch));
  });
  return value;
}

bool StartsWithInsensitive(const std::wstring& value,
                           const std::wstring& prefix) {
  return value.size() >= prefix.size() &&
         ToLower(value.substr(0, prefix.size())) == ToLower(prefix);
}

std::wstring QuoteArgument(const std::wstring& argument) {
  if (argument.empty()) {
    return L"\"\"";
  }
  if (argument.find_first_of(L" \t\n\v\"") == std::wstring::npos) {
    return argument;
  }

  std::wstring result = L"\"";
  size_t backslashes = 0;
  for (const wchar_t ch : argument) {
    if (ch == L'\\') {
      ++backslashes;
      continue;
    }
    if (ch == L'\"') {
      result.append(backslashes * 2 + 1, L'\\');
      result.push_back(L'\"');
      backslashes = 0;
      continue;
    }
    result.append(backslashes, L'\\');
    backslashes = 0;
    result.push_back(ch);
  }
  result.append(backslashes * 2, L'\\');
  result.push_back(L'\"');
  return result;
}

std::wstring BuildCommandLine(const std::vector<std::wstring>& arguments) {
  std::wstring command_line;
  for (size_t index = 0; index < arguments.size(); ++index) {
    if (index != 0) {
      command_line.push_back(L' ');
    }
    command_line.append(QuoteArgument(arguments[index]));
  }
  return command_line;
}

fs::path ExecutableDirectory() {
  std::wstring buffer(32768, L'\0');
  const DWORD length =
      GetModuleFileNameW(nullptr, buffer.data(), static_cast<DWORD>(buffer.size()));
  if (length == 0 || length >= buffer.size()) {
    return {};
  }
  buffer.resize(length);
  return fs::path(buffer).parent_path();
}

fs::path LocalProfileDirectory() {
  wchar_t buffer[MAX_PATH]{};
  const HRESULT result = SHGetFolderPathW(
      nullptr, CSIDL_LOCAL_APPDATA | CSIDL_FLAG_CREATE, nullptr,
      SHGFP_TYPE_CURRENT, buffer);
  if (FAILED(result) || buffer[0] == L'\0') {
    return {};
  }
  return fs::path(buffer) / kCompanyDirectory / kProfileDirectory / L"Profile";
}

fs::path NormalizeProfilePath(const fs::path& value) {
  if (value.empty()) {
    return {};
  }
  std::error_code error;
  fs::path normalized = fs::absolute(value, error);
  if (error || normalized.empty()) {
    return {};
  }
  return normalized.lexically_normal();
}

std::wstring ReadFirstLine(const fs::path& path) {
  std::wifstream stream(path);
  std::wstring line;
  if (!stream.good() || !std::getline(stream, line)) {
    return {};
  }
  while (!line.empty() && std::iswspace(line.back())) {
    line.pop_back();
  }
  while (!line.empty() && std::iswspace(line.front())) {
    line.erase(line.begin());
  }
  return line;
}

bool IsValidLocale(const std::wstring& locale) {
  if (locale.size() < 2 || locale.size() > 18) {
    return false;
  }
  for (const wchar_t ch : locale) {
    if (!((ch >= L'a' && ch <= L'z') || (ch >= L'A' && ch <= L'Z') ||
          (ch >= L'0' && ch <= L'9') || ch == L'-')) {
      return false;
    }
  }
  return true;
}

std::wstring WindowsErrorText(DWORD code) {
  LPWSTR raw = nullptr;
  const DWORD length = FormatMessageW(
      FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
          FORMAT_MESSAGE_IGNORE_INSERTS,
      nullptr, code, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
      reinterpret_cast<LPWSTR>(&raw), 0, nullptr);
  if (length == 0 || raw == nullptr) {
    return L"Windows error " + std::to_wstring(code);
  }
  std::wstring text(raw, length);
  LocalFree(raw);
  while (!text.empty() && std::iswspace(text.back())) {
    text.pop_back();
  }
  return text + L" (" + std::to_wstring(code) + L")";
}

void ReportError(const std::wstring& message, bool noninteractive) {
  if (noninteractive) {
    const std::wstring line = L"Ghosium Browser: " + message + L"\r\n";
    OutputDebugStringW(line.c_str());
    return;
  }
  MessageBoxW(nullptr, message.c_str(), kProductName,
              MB_OK | MB_ICONERROR | MB_SETFOREGROUND);
}

bool CoreFilesExist(const fs::path& root) {
  std::error_code error;
  const fs::path runtime = root / L"runtime";
  return fs::is_regular_file(runtime / kEngineExecutable, error) &&
         fs::is_regular_file(runtime / kTorExecutable, error) &&
         fs::is_regular_file(runtime / L"Tor\\data\\geoip", error) &&
         fs::is_regular_file(runtime / L"Tor\\data\\geoip6", error) &&
         fs::is_regular_file(root / L"LICENSE", error) &&
         fs::is_regular_file(root / L"THIRD_PARTY_NOTICES.md", error);
}

bool IsInternalSwitch(const std::wstring& argument) {
  const std::wstring lowered = ToLower(argument);
  return lowered == kSelfTestSwitch || lowered == kWaitSwitch ||
         StartsWithInsensitive(lowered, kPortableProfilePrefix) ||
         StartsWithInsensitive(lowered, kLanguagePrefix);
}

bool IsProtectedArgument(const std::wstring& argument, bool* consumes_next) {
  *consumes_next = false;
  const std::wstring lowered = ToLower(argument);
  const std::vector<std::wstring> valued = {
      L"-profile", L"--profile", L"-p", L"--profilemanager",
      L"-start-debugger-server", L"--start-debugger-server", L"-uilocale"};

  for (const auto& option : valued) {
    if (lowered == option) {
      *consumes_next = true;
      return true;
    }
    if (StartsWithInsensitive(lowered, option + L"=")) {
      return true;
    }
  }

  return lowered == L"-profilemanager" ||
         lowered == L"--profilemanager" ||
         lowered == L"-marionette" ||
         lowered == L"--marionette" ||
         lowered == L"-jsconsole" ||
         lowered == L"--jsconsole" ||
         lowered == L"-devtools" ||
         lowered == L"--devtools" || IsInternalSwitch(lowered);
}

bool IsValidHandle(HANDLE handle) {
  return handle != nullptr && handle != INVALID_HANDLE_VALUE;
}

HANDLE DuplicateForChild(HANDLE source) {
  if (!IsValidHandle(source)) {
    return nullptr;
  }
  HANDLE duplicate = nullptr;
  if (!DuplicateHandle(GetCurrentProcess(), source, GetCurrentProcess(),
                       &duplicate, 0, TRUE, DUPLICATE_SAME_ACCESS)) {
    return nullptr;
  }
  return duplicate;
}

void ApplyLauncherMitigations() {
  SetDllDirectoryW(L"");
  PROCESS_MITIGATION_IMAGE_LOAD_POLICY policy{};
  policy.NoRemoteImages = 1;
  policy.NoLowMandatoryLabelImages = 1;
  SetProcessMitigationPolicy(ProcessImageLoadPolicy, &policy, sizeof(policy));
}

bool InitializeSockets() {
  WSADATA data{};
  return WSAStartup(MAKEWORD(2, 2), &data) == 0;
}

uint16_t FindAvailableLoopbackPort() {
  SOCKET socket_handle = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
  if (socket_handle == INVALID_SOCKET) {
    return 0;
  }

  sockaddr_in address{};
  address.sin_family = AF_INET;
  address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
  address.sin_port = 0;

  if (bind(socket_handle, reinterpret_cast<sockaddr*>(&address),
           sizeof(address)) == SOCKET_ERROR) {
    closesocket(socket_handle);
    return 0;
  }

  int length = sizeof(address);
  if (getsockname(socket_handle, reinterpret_cast<sockaddr*>(&address),
                  &length) == SOCKET_ERROR) {
    closesocket(socket_handle);
    return 0;
  }

  const uint16_t port = ntohs(address.sin_port);
  closesocket(socket_handle);
  return port;
}

bool WaitForLoopbackPort(uint16_t port, HANDLE process, DWORD timeout_ms) {
  const auto deadline =
      std::chrono::steady_clock::now() + std::chrono::milliseconds(timeout_ms);

  while (std::chrono::steady_clock::now() < deadline) {
    if (WaitForSingleObject(process, 0) == WAIT_OBJECT_0) {
      return false;
    }

    SOCKET socket_handle = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (socket_handle != INVALID_SOCKET) {
      sockaddr_in address{};
      address.sin_family = AF_INET;
      address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
      address.sin_port = htons(port);
      if (connect(socket_handle, reinterpret_cast<sockaddr*>(&address),
                  sizeof(address)) == 0) {
        closesocket(socket_handle);
        return true;
      }
      closesocket(socket_handle);
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(250));
  }
  return false;
}

bool WriteManagedTorPrefs(const fs::path& profile_directory, uint16_t socks_port) {
  const fs::path user_js = profile_directory / L"user.js";
  const std::vector<std::string> managed_keys = {
      "network.proxy.type",
      "network.proxy.socks",
      "network.proxy.socks_port",
      "network.proxy.socks_version",
      "network.proxy.socks_remote_dns",
      "network.proxy.no_proxies_on",
      "network.proxy.failover_direct",
      "network.trr.mode",
  };

  std::vector<std::string> preserved;
  {
    std::ifstream input(user_js);
    std::string line;
    while (input.good() && std::getline(input, line)) {
      bool managed = false;
      for (const auto& key : managed_keys) {
        if (line.find("user_pref(\"" + key + "\"") != std::string::npos) {
          managed = true;
          break;
        }
      }
      if (!managed) {
        preserved.push_back(line);
      }
    }
  }

  std::ofstream output(user_js, std::ios::trunc);
  if (!output.good()) {
    return false;
  }
  for (const auto& line : preserved) {
    output << line << "\n";
  }
  output << "user_pref(\"network.proxy.type\", 1);\n";
  output << "user_pref(\"network.proxy.socks\", \"127.0.0.1\");\n";
  output << "user_pref(\"network.proxy.socks_port\", " << socks_port << ");\n";
  output << "user_pref(\"network.proxy.socks_version\", 5);\n";
  output << "user_pref(\"network.proxy.socks_remote_dns\", true);\n";
  output << "user_pref(\"network.proxy.no_proxies_on\", \"\");\n";
  output << "user_pref(\"network.proxy.failover_direct\", false);\n";
  output << "user_pref(\"network.trr.mode\", 5);\n";
  return output.good();
}

bool LaunchTor(const fs::path& runtime_directory,
               const fs::path& profile_directory,
               uint16_t socks_port,
               PROCESS_INFORMATION* process_info) {
  const fs::path tor_directory = runtime_directory / L"Tor";
  const fs::path tor_executable = tor_directory / L"tor.exe";
  const fs::path tor_data = profile_directory / L"TorData";
  std::error_code error;
  fs::create_directories(tor_data, error);
  if (error) {
    return false;
  }

  std::vector<std::wstring> arguments = {
      tor_executable.wstring(),
      L"--SocksPort",
      L"127.0.0.1:" + std::to_wstring(socks_port),
      L"--ClientOnly",
      L"1",
      L"--AvoidDiskWrites",
      L"1",
      L"--SafeSocks",
      L"1",
      L"--DataDirectory",
      tor_data.wstring(),
      L"--GeoIPFile",
      (tor_directory / L"data\\geoip").wstring(),
      L"--GeoIPv6File",
      (tor_directory / L"data\\geoip6").wstring(),
      L"--__OwningControllerProcess",
      std::to_wstring(GetCurrentProcessId()),
  };

  std::wstring command_line = BuildCommandLine(arguments);
  STARTUPINFOW startup_info{};
  startup_info.cb = sizeof(startup_info);
  ZeroMemory(process_info, sizeof(*process_info));

  const BOOL created = CreateProcessW(
      tor_executable.c_str(), command_line.data(), nullptr, nullptr, FALSE,
      CREATE_NO_WINDOW | CREATE_UNICODE_ENVIRONMENT | CREATE_DEFAULT_ERROR_MODE,
      nullptr, tor_directory.c_str(), &startup_info, process_info);
  if (!created) {
    return false;
  }
  CloseHandle(process_info->hThread);
  process_info->hThread = nullptr;
  return true;
}

void StopManagedTor(PROCESS_INFORMATION* process_info) {
  if (!process_info || !IsValidHandle(process_info->hProcess)) {
    return;
  }
  if (WaitForSingleObject(process_info->hProcess, 0) != WAIT_OBJECT_0) {
    TerminateProcess(process_info->hProcess, 0);
    WaitForSingleObject(process_info->hProcess, 5000);
  }
  CloseHandle(process_info->hProcess);
  process_info->hProcess = nullptr;
}

}  // namespace

int APIENTRY wWinMain(HINSTANCE, HINSTANCE, LPWSTR, int) {
  ApplyLauncherMitigations();

  int argc = 0;
  LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &argc);
  if (argv == nullptr) {
    ReportError(L"Ghosium Browser could not read the launch command.", false);
    return 3;
  }

  bool self_test = false;
  bool wait_for_engine = false;
  bool headless_mode = false;
  fs::path portable_profile;
  std::wstring requested_locale;

  for (int index = 1; index < argc; ++index) {
    const std::wstring argument = argv[index];
    const std::wstring lowered = ToLower(argument);
    if (lowered == kSelfTestSwitch) {
      self_test = true;
    } else if (lowered == kWaitSwitch) {
      wait_for_engine = true;
    } else if (lowered == L"--dump-dom" ||
               StartsWithInsensitive(lowered, L"--headless")) {
      headless_mode = true;
      wait_for_engine = true;
    } else if (StartsWithInsensitive(argument, kPortableProfilePrefix)) {
      portable_profile = fs::path(
          argument.substr(std::wstring(kPortableProfilePrefix).size()));
    } else if (StartsWithInsensitive(argument, kLanguagePrefix)) {
      requested_locale =
          argument.substr(std::wstring(kLanguagePrefix).size());
    }
  }

  const bool noninteractive = self_test || wait_for_engine || headless_mode;
  const fs::path root = ExecutableDirectory();
  if (root.empty() || !CoreFilesExist(root)) {
    LocalFree(argv);
    ReportError(
        L"Ghosium Browser files are incomplete. Reinstall or download a fresh official package.",
        noninteractive);
    return 2;
  }

  if (self_test) {
    LocalFree(argv);
    return 0;
  }

  const fs::path runtime_directory = root / L"runtime";
  const fs::path engine_executable = runtime_directory / kEngineExecutable;

  fs::path profile_directory = portable_profile.empty()
                                   ? LocalProfileDirectory()
                                   : NormalizeProfilePath(portable_profile);
  if (profile_directory.empty()) {
    LocalFree(argv);
    ReportError(
        L"Ghosium Browser could not resolve a safe local profile directory.",
        noninteractive);
    return 4;
  }

  std::error_code directory_error;
  fs::create_directories(profile_directory, directory_error);
  if (directory_error) {
    LocalFree(argv);
    ReportError(
        L"Ghosium Browser could not prepare the selected local profile. Check folder permissions and try again.",
        noninteractive);
    return 4;
  }

  if (!InitializeSockets()) {
    LocalFree(argv);
    ReportError(L"Ghosium Browser could not initialize its local Tor transport.",
                noninteractive);
    return 7;
  }

  const uint16_t socks_port = FindAvailableLoopbackPort();
  if (socks_port == 0 || !WriteManagedTorPrefs(profile_directory, socks_port)) {
    WSACleanup();
    LocalFree(argv);
    ReportError(L"Ghosium Browser could not prepare the Tor-only browser profile.",
                noninteractive);
    return 7;
  }

  PROCESS_INFORMATION tor_process{};
  if (!LaunchTor(runtime_directory, profile_directory, socks_port, &tor_process)) {
    WSACleanup();
    LocalFree(argv);
    ReportError(L"Ghosium Browser could not start the bundled Tor runtime.",
                noninteractive);
    return 7;
  }

  if (!WaitForLoopbackPort(socks_port, tor_process.hProcess, 60000)) {
    StopManagedTor(&tor_process);
    WSACleanup();
    LocalFree(argv);
    ReportError(
        L"Ghosium Browser could not establish its local Tor SOCKS transport. Browser startup was blocked to avoid a direct-network fallback.",
        noninteractive);
    return 8;
  }

  std::wstring locale = requested_locale;
  if (locale.empty()) {
    locale = ReadFirstLine(root / L"ghosium-language.txt");
  }
  if (!IsValidLocale(locale)) {
    locale = L"en-US";
  }

  std::vector<std::wstring> arguments;
  arguments.emplace_back(engine_executable.wstring());
  arguments.emplace_back(L"-profile");
  arguments.emplace_back(profile_directory.wstring());
  arguments.emplace_back(L"-UILocale");
  arguments.emplace_back(locale);

  for (int index = 1; index < argc; ++index) {
    bool consumes_next = false;
    if (IsProtectedArgument(argv[index], &consumes_next)) {
      if (consumes_next && index + 1 < argc) {
        ++index;
      }
      continue;
    }
    arguments.emplace_back(argv[index]);
  }
  LocalFree(argv);

  std::wstring command_line = BuildCommandLine(arguments);
  STARTUPINFOW startup_info{};
  startup_info.cb = sizeof(startup_info);
  BOOL inherit_handles = FALSE;
  HANDLE child_stdin = nullptr;
  HANDLE child_stdout = nullptr;
  HANDLE child_stderr = nullptr;

  if (headless_mode) {
    child_stdin = DuplicateForChild(GetStdHandle(STD_INPUT_HANDLE));
    child_stdout = DuplicateForChild(GetStdHandle(STD_OUTPUT_HANDLE));
    child_stderr = DuplicateForChild(GetStdHandle(STD_ERROR_HANDLE));
    if (child_stdout && child_stderr) {
      startup_info.dwFlags |= STARTF_USESTDHANDLES;
      startup_info.hStdInput = child_stdin;
      startup_info.hStdOutput = child_stdout;
      startup_info.hStdError = child_stderr;
      inherit_handles = TRUE;
    }
  }

  PROCESS_INFORMATION process_info{};
  const BOOL created = CreateProcessW(
      engine_executable.c_str(), command_line.data(), nullptr, nullptr,
      inherit_handles, CREATE_UNICODE_ENVIRONMENT | CREATE_DEFAULT_ERROR_MODE,
      nullptr, runtime_directory.c_str(), &startup_info, &process_info);

  if (child_stdin) CloseHandle(child_stdin);
  if (child_stdout) CloseHandle(child_stdout);
  if (child_stderr) CloseHandle(child_stderr);

  if (!created) {
    const DWORD error = GetLastError();
    StopManagedTor(&tor_process);
    WSACleanup();
    ReportError(L"Ghosium Browser could not start its Firefox/Tor Browser engine. " +
                    WindowsErrorText(error),
                noninteractive);
    return 5;
  }

  CloseHandle(process_info.hThread);

  // The Ghosium launcher deliberately remains alive for the browser lifetime.
  // This keeps the bundled Tor process owned by the same public app process and
  // ensures Tor is stopped when the corresponding Firefox instance exits.
  WaitForSingleObject(process_info.hProcess, INFINITE);
  DWORD exit_code = 0;
  if (!GetExitCodeProcess(process_info.hProcess, &exit_code)) {
    exit_code = 6;
  }
  CloseHandle(process_info.hProcess);
  StopManagedTor(&tor_process);
  WSACleanup();

  return wait_for_engine || headless_mode ? static_cast<int>(exit_code) : 0;
}
