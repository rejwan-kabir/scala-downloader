[//]: https://i.pinimg.com/564x/5c/46/e4/shuvo.jpg,ftp://speedtest.tele2.net/512KB.zip,sftp://rejwan:password@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145004.jpg,http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg,ftp://speedtest.tele2.net/2MB.zip,https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg,sftp://rejwan:password@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_144432.jpg,https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg,ftp://speedtest.tele2.net/3MB.zip,https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg,https://i.pinimg.com/564x/5c/46/e4/imon.jpg,ftp://speedtest.tele2.net/5MB.zip,https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg,https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg,sftp://rejwan:password@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145034.jpg,https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg,https://i.pinimg.com/564x/5c/46/e4/arshi.jpg

The command to run the downloader is

```$xslt
sbt "runmain demo.Downloader <url-list> <download-directory-path>"
```

url-list may contain `http`, `https`, `ftp`, `ftps`, `sftp` files. Multiple URL must be separated with a comma.

Template for sftp urls with password : 
```$xslt
sftp://username:password@host:<port>/path/to/file.ext
```

Downloader with wait for 30 seconds, would skip that particular download if no response found for consecutive 30 seconds.