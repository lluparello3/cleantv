# CleanTV — Casa Ciompi

Launcher minimale per Chromecast con Google TV / Google TV Streamer: solo una griglia delle app installate, senza righe di contenuti, suggerimenti o pubblicità.

- Frecce del telecomando per muoversi, OK per aprire un'app.
- Tieni premuto OK su un'app per nasconderla dalla griglia (resta installata).
- Tieni premuto OK su **Impostazioni** per mostrare di nuovo le app nascoste.

## Installazione sul Chromecast

1. Sulla TV installa **Downloader** (di AFTVnews) dal Play Store.
2. In *Impostazioni → Sistema → Opzioni sviluppatore* (o *App → Sicurezza e limitazioni → Origini sconosciute*) consenti a Downloader di installare app.
3. In Downloader apri il link `https://github.com/<utente>/<repo>/releases/latest/download/CleanTV.apk` e conferma l'installazione.
4. Apri **CleanTV** dalla lista delle app.

Ogni push su `main` ricompila l'APK con GitHub Actions e aggiorna lo stesso link.

## Tasto Home

Su Google TV il tasto Home apre sempre la home di Google finché il launcher di sistema è attivo. Per farlo arrivare a CleanTV bisogna disattivare quello di Google via ADB (per esempio dall'app LADB o Bugjaeger su un telefono Android):

```
pm disable-user --user 0 com.google.android.apps.tv.launcherx
pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

Per tornare indietro: `pm enable com.google.android.apps.tv.launcherx` e `pm enable com.google.android.tungsten.setupwraith`.
