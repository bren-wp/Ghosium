# Ghosium Android — sigurnosne napomene

- Browser prikazuje stranice preko Android System WebView, a ne preko zasebnog browser enginea.
- JavaScript je omogućen zbog kompatibilnosti modernog weba. Nema JavaScript interfacea prema Androidu.
- Zabranjeni su cleartext HTTP, datotečni URL-ovi, univerzalni pristup lokalnim datotekama i mixed content.
- Nevaljani SSL certifikati rezultiraju prekidom učitavanja; Safe Browsing vodi natrag na sigurnu stranicu.
- Nije podržana anonimnost, izolirani incognito profil ili stvarni ad/tracker blocking.
- Web dopuštenja za kameru/mikrofon odbijaju se, geolokacija je onemogućena.
- Sadržaj i prijenos datoteka dodatno zahtijevaju reviziju protiv zloupotrebe, prije produkcije.
- Prijavljivanje ranjivosti: ne uključivati privatne ključeve ili korisničke podatke u javne issuee.

**Status**: razvojni projekt. Ne predstavljati kao sigurnosno certificiran preglednik.
