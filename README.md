# yopenedi
OpenTrans 2.1/EDIFACT D.96A File converter
1. Convert Files to and from OpenTrans 2.1/[EDIFACT D.96A](http://www.unece.org/trade/untdid/d96a/content.htm)
2. [Noname order sample from REXEL](https://1drv.ms/u/s!AgbWsnOPcbiN7iyEebz5pdfK6Arz?e=p9vhYD)
3. [Sample XML Files from OpenTrans 2.1](https://1drv.ms/u/s!AgbWsnOPcbiN7hNOj5J96OsS2svQ?e=OnPvPZ)
4. Send files as attachment in EDIFACT D.96A format to xmlxonv@ywesee.com and convert them to the OpenTrans 2.1 format.
5. Upload converted files to an FTP-Server.
6. Import OpenTrans 2.1 files from FTP and convert them to EDIFACT D.96A and send the converted file as Email-Attachment from xmlxonv@ywesee.com to a dedicated Email address.
7. Send Opentrans 2.1 XML files to this [URL](https://connect.boni.ch/OpaccOne/B2B/Channel/XmlOverHttp/ywesee)
8. Receive Opentrans 2.1 XML files at this [URL](https://yopenedi.ch/input)

## FAQ
* [https://stackoverflow.com/questions/11295551/is-there-a-really-simple-way-to-process-edifact-for-example-d96a](Stackoverflow)

## Certbot for Ubuntu 20.04
* https://certbot.eff.org/lets-encrypt/ubuntufocal-other

## Digital Ocean deployment
### Apache setup
```
<VirtualHost *:80>
  ServerName test.yopenedi.ch
  Redirect permanent / https://test.yopenedi.ch
</VirtualHost>

<VirtualHost 104.248.255.2:443>
  ServerName test.yopenedi.ch
  ProxyPreserveHost On
  ProxyPass  /excluded !
  ProxyPass / http://127.0.0.1:3000/
  ProxyPassReverse / http://127.0.0.1:3000/
  SSLEngine on
  SSLCertificateFile /etc/letsencrypt/live/test.yopenedi.ch/cert.pem
  SSLCertificateKeyFile /etc/letsencrypt/live/test.yopenedi.ch/privkey.pem
  SSLCertificateChainFile /etc/letsencrypt/live/test.yopenedi.ch/chain.pem
</VirtualHost>
```
