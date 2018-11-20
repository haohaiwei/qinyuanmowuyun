package main
import (
    "context"
    "fmt"
    "github.com/qiniu/api.v7/auth/qbox"
    "github.com/qiniu/api.v7/storage"
)
type MyPutRet struct {
    Key    string
    Hash   string
    Fsize  int
    Bucket string
    Name   string
}
var(
    accessKey = "LikMBDoxQUgNxA2f8RBpNDxs3BOb9pfCUqJMNxay"
    secretKey = "rSCw7_qT0qLHKhbY-zcJuyuqC3_C5pGQw0xe_gqM"
    bucket = "zsxnbucket1"
)
func main(){
    localFile := "/Users/haohaiwei/s3.txt"
    key := "test.txt"
    putPolicy := storage.PutPolicy{
    Scope:      bucket,
    ReturnBody: `{"key":"$(key)","hash":"$(etag)","fsize":$(fsize),"bucket":"$(bucket)","name":"$(x:name)"}`,
    }
    mac := qbox.NewMac(accessKey, secretKey)
    upToken := putPolicy.UploadToken(mac)
    zone := storage.Zone{
            SrcUpHosts:[]string{"up-qos.tc.echosoul.cn"},
            CdnUpHosts:[]string{"up-qos.tc.echosoul.cn"},
            RsHost:"rs-qos.tc.echosoul.cn",
            RsfHost:"rsf-qos.tc.echosoul.cn",
            IovipHost:"io-qos.tc.echosoul.cn",
            ApiHost:"api-qos.tc.echosoul.cn",
    }
    cfg := storage.Config{}
    cfg.Zone = &zone
    formUploader := storage.NewFormUploader(&cfg)
    ret := MyPutRet{}
    putExtra := storage.PutExtra{
        Params: map[string]string{
            "x:name": "github logo",
        },
    }
    err := formUploader.PutFile(context.Background(), &ret, upToken, key, localFile, &putExtra)
    if err != nil {
        fmt.Println(err)
        return
    }
    fmt.Println(ret.Bucket, ret.Key, ret.Fsize, ret.Hash, ret.Name)
 }   
