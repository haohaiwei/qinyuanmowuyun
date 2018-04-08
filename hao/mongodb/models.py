# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models

# Create your models here.
from mongoengine import * 

connect('zhilian', host='192.168.1.221', port=27003)

class ArtiInfo(Document): 
    zw_link = StringField() 
    gzdd = StringField() 
    zwmc = StringField() 
    gsmc = StringField() 
    brief = StringField() 
    _id = StringField()
    fkl = StringField()
    gbsj = StringField()
    save_date = StringField()
    zwyx = StringField()
    area = ListField(StringField()) # 定义列表类型 
    cates = ListField(StringField()) 

    meta = { 'collection': 'python'} # 指明连接数据库的哪张表 

for i in ArtiInfo.objects[:10]: # 测试是否连接成功 
    print(i)
