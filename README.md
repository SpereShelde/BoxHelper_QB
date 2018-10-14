# BoxHelper
# 这是 Pro 分支，只支持qBittorrent客户端; This is pro branch, which only support qBittorrent
## 使用; How to use

克隆仓库; Clone this repository:

`git clone -b pro https://github.com/SpereShelde/BoxHelper.git && cd BoxHelper`

安装环境，仅使用一次; Build environment for the first time: `bash java.sh`

编辑配置; Edit confiure file：`vi config.json`

添加Cookie文件; Import cookie file：

在cookies目录下，使用json格式保存您的站点cookie，命名为`站点域名`.json

Create a file under 'cookies', names 'WEBSITE-DOMAIN'.json, to save cookie.

可以添加多个Cookie文件, You can add several cookie files. 

开启后台; Create a background bash：`screen -R BoxHelper`

运行BoxHelper; Run BoxHelper：`java -jar BoxHelper.jar`

Ctrl + a + d 退出screen后台; Type Ctrl + a + d to exit;

---

升级(替换BoxHelper.jar); Upgrade(Download new BoxHelper.jar)：

`wget 'https://github.com/SpereShelde/BoxHelper/blob/master/BoxHelper.jar?raw=true' -O BoxHelper.jar`

## 目前完成度; Status

- Support NexusPHP.

- ONLY Support qBittorrent.

- 可选择监听页面, 并限制种子大小和上传下载速度; You can choose pages to listen, and limit the torrent size and transfer speed

## 注意事项; Watch this！ 

- BoxHelper 需要你提供Cookie，并会获取你的passkey，但是 BoxHelper 不会上传他们

- BoxHelper needs your cookie and will acquire your passkey, but BoxHelper will not upload them.

- 账号有限，无法测试更多站点。如果确定 BoxHelper 支持 或 不支持 你所使用的站点，请告诉我，谢谢

- We cannot test all sites. Please help us to test and tell me the result, thank you!

- 反馈问题或希望适配其他站点，请发Issue或联系我: `spereshelde#gmail.com`

- To feedback bugs or want us to test more sites, just open an issue or mail me at `spereshelde#gmail.com`

