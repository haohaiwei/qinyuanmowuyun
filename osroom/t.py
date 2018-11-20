#coding: utf-8  
import smtplib
from email.mime.text import MIMEText
from email.header import Header

sender = 'haohaiwei@126.com'
receiver = 'haohaiwei@126.com'
subject = 'python email test'
smtpserver = 'smtp.126.com'
username = 'haohaiwei@126.com'
password = 'hhw199202'

msg = MIMEText( 'Hello Python', 'text', 'utf-8' )
msg['Subject'] = Header( subject, 'utf-8' )

smtp = smtplib.SMTP()
smtp.connect( smtpserver )
smtp.login( username, password )
smtp.sendmail( sender, receiver, msg.as_string() )
smtp.quit()
