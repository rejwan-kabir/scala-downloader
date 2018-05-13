#### Build and Run Instruction

The command to run the downloader is

```
sbt "runMain demo.Downloader <url-list> <download-directory-path>"
```

url-list may contain `http`, `https`, `ftp`, `ftps`, `sftp` files. Multiple URL must be separated with a comma.

Template for sftp urls with password : 
```$xslt
sftp://username:password@host:<port>/path/to/file.ext
```

Downloader with wait for 30 seconds, would skip that particular download if no response found for consecutive 30 seconds.

#### Testing

```
sbt test
```