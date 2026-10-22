# Ghosium QA check-list

Status: **Početni MVP; neprovjeren build u korisničkom okruženju.**

- [ ] GitHub Actions Android CI potvrđen zelenim testovima
- [ ] Instalacijski APK testiran na stvarnom Android telefonu
- [ ] Natrag / naprijed, switch kartica, zatvaranje kartica i crash renderer
- [ ] HTTP→HTTPS preusmjeravanja, blokada SSL pogrešaka
- [ ] Downloads: putanja, MIME, cookie, status, duplicirani nazivi
- [ ] WebView soft keyboard, orijentacija, accessibility, veliki fontovi
- [ ] Brisanje kolačića/povijesti/predmemorije, race condition uz callbackove
- [ ] Upit/URL normalizacija, IDN, IPv6, netipične sheme, localhost, kodiranje
- [ ] Privatne kartice (poseban data directory process) tek razviti
- [ ] Ad/tracker blocking tek razviti uz razumno predstavljanje mogućnosti
- [ ] Dizajn svih 20 ekrana implementirati u native UI bez lažnih opcija
- [ ] Pravila Google Playa, Data Safety, policy link, identitet aplikacije, signed AAB

Android manifest i Gradle parametri su sastavljeni, ali ova sesija ne može potvrditi proizvodni build na stvarnom uređaju.
