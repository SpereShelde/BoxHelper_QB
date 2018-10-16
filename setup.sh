#!/bin/bash

#=================================================
#	Description: BoxHelper
#	Version: 0.0.1
#	Author: SpereShelde
#=================================================

#读取配置文件
read_config(){
    webUI=$(cat config.json | jq '.webUI')
    sessionID=$(cat config.json | jq '.sessionID')
    diskLimit=$(cat config.json | jq '.diskLimit')
    actionAfterLimit=$(cat config.json | jq '.actionAfterLimit')
    runningCycleInSec=$(cat config.json | jq '.runningCycleInSec')
    urls=$(cat config.json | jq '.urls[]')
}
#获取PID
check_pid(){
	PID=`ps -ef| grep "BoxHelper"| grep -v grep| grep -v ".sh"| grep -v "init.d"| grep -v "service"| awk '{print $2}'`
}
install_boxhelper(){
    echo -e "开始下载 BoxHelper ..."
    git clone https://github.com/SpereShelde/BoxHelper_QB.git
    cd BoxHelper_QB
    jdk_status=$(java -version 2>&1)
    if [[ ${jdk_status/"1.8"//} == $jdk_status ]]
        then
            echo "没有安装 JDK 1.8, 开始安装 ..."
            chmod +x java.sh
            bash java.sh
    fi
    echo -e "开始编辑 BoxHelper 配置文件 ..."
    add_config
    start_boxhelper
}
add_urls(){
    n=0
    while true
        do
        echo -e " 请输入要监听的种子页面:"
        read -e -p " (默认: 停止添加):" page[$n]
        [[ -z "${page[$n]}" ]] && echo -e "停止添加监控页面..." && break
        domain[$n]=$(echo ${page[$n]} | awk -F'[/:]' '{print $4}')
        if [ -e cookies/$domain[$n] ]; then
          echo " 存有此页面Cookie, 下一步..."
        else
          echo " 请以Json格式输入此站点的Cookie:"
          read -e -p " (默认: 取消):" cookie[$n]
          [[ -z "${page[$n]}" ]] && echo -e "已取消..." && exit 1
          echo "$cookie[$n]">cookies/$domain[$n]
        fi
        echo -e " 请输入此页面筛选的种子最小体积, 单位为GB, -1 为不限制:"
        read -e -p " (默认: -1):" lower[$n]
        echo -e " 请输入此页面筛选的种子最大体积, 单位为GB, -1 为不限制:"
        read -e -p " (默认: -1):" higher[$n]
        echo -e " 请输入此页面下载的种子下载限速, 单位为MB/s, -1 为不限制:"
        read -e -p " (默认: -1):" download[$n]
        echo -e " 请输入此页面下载的种子上传限速, 单位为MB/s, -1 为不限制:"
        read -e -p " (默认: -1):" upload[$n]
        let n++
    done
    let n--
}
add_config(){
    echo -e "为了简化操作，请提前查看Wiki: https://github.com/SpereShelde/BoxHelper_QB/wiki"
    echo -e " 请输入 qBittorrent Web UI:"
    read -e -p " (默认: 取消):" web
    [[ -z "$web" ]] && echo "已取消..." && exit 1
    echo -e " 请输入 qBittorrent Web UI 的 SID:"
    read -e -p " (默认: 取消):" sid
    [[ -z "$sid" ]] && echo "已取消..." && exit 1
    echo -e " 请输入 BoxHelper 下载种子的磁盘使用上限, 单位为GB:"
    read -e -p " (默认: 1024):" disk
    echo -e " 请输入 BoxHelper 下载种子达到磁盘使用上限之后的删除策略:"
    read -e -p " (默认: large):" action
    [[ -z "$action" ]] && action="large"
    echo -e " 请输入 BoxHelper 按照上述策略删除的种子个数:"
    read -e -p " (默认: 2):" amount
    [[ -z "$amount" ]] && amount="2"
    echo -e " 请输入 BoxHelper 监听周期, 单位为秒:"
    read -e -p " (默认: 20):" cycle
    [[ -z "$cycle" ]] && cycle="20"
    echo -e " 请输入 BoxHelper 是否载入运行之前已经存在的Free种 [y/n]:"
    read -e -p " (默认: n):" load
    if [[ "${load}" == [Yy] ]]; then load=true
    else
        load=false
    fi

    add_urls

    page_len=${#page[*]}
    let page_len--
    i=0
    while [ $i -lt $page_len ]
        do
        if [ $i == 0 ]
        then
           urls="[\"${page[$i]}\", \"${lower[$i]}\", \"${higher[$i]}\", \"${download[$i]}\", \"${upload[$i]}\"]"
        else
           urls=$urls", [\"${page[$i]}\", \"${lower[$i]}\", \"${higher[$i]}\", \"${download[$i]}\", \"${upload[$i]}\"]"
        fi
        let i++
    done

    echo "{
            \"webUI\":\"$web\",
            \"sessionID\":\"$sid\",
            \"diskLimit\":$disk,
            \"actionAfterLimit\":[\"$amount\", \"$action\"],
            \"runningCycleInSec\":$cycle,
            \"urls\":[
                 $urls
            ],
            \"load\":$load
    }">config.json
}
get_config(){
    config_webUI=`cat config.json | jq '.webUI'`
    config_sessionID=`cat config.json | jq '.sessionID'`
    config_diskLimit=`cat config.json | jq '.diskLimit'`
    config_actionAfterLimit=`cat config.json | jq '.actionAfterLimit[]'`
    config_action=`echo $config_actionAfterLimit | cut -d " " -f 2 | cut -d '"' -f 2`
    config_amount=`echo $config_actionAfterLimit | cut -d " " -f 1 | cut -d '"' -f 2`
    config_runningCycleInSec=`cat config.json | jq '.runningCycleInSec'`
    config_urls=`cat config.json | jq '.urls[][]'`
    array=(${config_urls//'" "'/ })

    config_page_num=`echo $config_urls | grep -o '" "' |wc -l`
    let config_page_num++
    let config_page_num/=5
}
edit_boxhelper(){
    jq_status=$(jq --help)
    if [[ -z ${jq_status} ]]; then
        echo "准备中 ..."
        wget 'https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64'
        mv jq-linux64 jq
        chmod +x jq
        mv jq /bin/
    fi
    get_config
    echo "  当前配置:"
    echo "  qBittorrent Web UI            : $config_webUI"
    echo "  qBittorrent Web UI 的 SID     : $config_sessionID"
    echo "  BoxHelper 的磁盘使用上限      : $config_diskLimit GB"
    echo "  BoxHelper 的删除策略          : $config_action"
    #--!--#
    echo "  BoxHelper 的删除个数          : $config_amount 个"
    echo "  BoxHelper 的监听周期          : $config_runningCycleInSec"
    i=0
    len=${#array[*]}
    while [ $i -lt $len ]
        do
           echo "  BoxHelper 的监听页面 $[i/5+1]  : 监控 ${array[$i]} 内 大于 ${array[$[i+1]]} GB 且小于 ${array[$[i+2]]} GB 的种子，限制下载速度为 ${array[$[i+3]]} MB/s 上传速度为 ${array[$[i+4]]} MB/s "
           let i+=5
    done
    echo -e "
${Green_font_prefix} 1.${Font_color_suffix} 修改 qBittorrent Web UI
${Green_font_prefix} 2.${Font_color_suffix} 修改 qBittorrent Web UI 的 SID
${Green_font_prefix} 3.${Font_color_suffix} 修改 BoxHelper 的磁盘使用上限
${Green_font_prefix} 4.${Font_color_suffix} 修改 BoxHelper 的删除策略及个数
${Green_font_prefix} 5.${Font_color_suffix} 修改 BoxHelper 的监听周期
${Green_font_prefix} 6.${Font_color_suffix} 修改 BoxHelper 的监听页面及相关限制" && echo
    read -e -p " 请输入数字 [1-6]:" num
    case "$num" in
    	1)
    	edit_webui
    	;;
    	2)
    	edit_sid
    	;;
    	3)
    	edit_disk
    	;;
    	4)
    	edit_action
    	;;
    	5)
    	edit_cycle
    	;;
    	6)
    	edit_urls
    	;;
    	*)
    	echo "请输入正确数字 [1-6]"
    	;;
    esac
    echo
    echo -e " 是否重启 BoxHelper 来加载配置 [y/n]:"
    read -e -p " (默认: y):" reboot
    if [[ "${reboot}" == [Yy] ]]; then restart_boxhelper
        else
            exit 1
    fi

}
edit_webui(){
    echo
    echo -e " 请输入 qBittorrent Web UI:"
    read -e -p " (默认: 取消):" web
    [[ -z "$web" ]] && echo "已取消..." && exit 1
    sed -i  's/\("webUI":"\).*/\1'"$web"'",/g'   config.json

}
edit_sid(){
    echo
    echo -e " 请输入 qBittorrent Web UI 的 SID:"
    read -e -p " (默认: 取消):" sid
    [[ -z "$sid" ]] && echo "已取消..." && exit 1
    sed -i  's/\("sessionID":"\).*/\1'"$sid"'",/g'   config.json

}
edit_disk(){
    echo
    echo -e " 修改 BoxHelper 的磁盘使用上限, 单位是 GB:"
    read -e -p " (默认: 取消):" disk
    [[ -z "$disk" ]] && echo "已取消..." && exit 1
    sed -i  's/\("diskLimit":\).*/\1'"$disk"',/g'   config.json

}
edit_action(){
    echo
    echo -e " 请输入 BoxHelper 下载种子达到磁盘使用上限之后的删除策略:"
    read -e -p " (默认: large):" action
    [[ -z "$action" ]] && action="large"
    echo -e " 请输入 BoxHelper 按照上述策略删除的种子个数:"
    read -e -p " (默认: 2):" amount
    [[ -z "$amount" ]] && amount="2"
    sed -i  's/\("actionAfterLimit":\["\).*/\1'"$amount"\","\"$action"\"\],'/g'   config.json
}
edit_cycle(){
    echo
    echo -e " 修改 BoxHelper 的监听周期, 单位是 秒:"
    read -e -p " (默认: 取消):" cycle
    [[ -z "$cycle" ]] && echo "已取消..." && exit 1
    sed -i  's/\("runningCycleInSec":\).*/\1'"$cycle"',/g'   config.json

}
edit_urls(){
    get_config
    i=0
    while [ $i -lt $len ]
        do
           echo "  BoxHelper 的监听页面 $[i/5+1]  : 监控 ${array[$i]} 内 大于 ${array[$[i+1]]} GB 且小于 ${array[$[i+2]]} GB 的种子，限制下载速度为 ${array[$[i+3]]} MB/s 上传速度为 ${array[$[i+4]]} MB/s "
           let i+=5
    done
    echo && echo -e "
    ${Green_font_prefix} 1.${Font_color_suffix} 添加 BoxHelper 监控页面
    ${Green_font_prefix} 2.${Font_color_suffix} 删除 BoxHelper 监控页面" && echo
    read -e -p " 请输入数字 [1-2]:" num
    case "$num" in
        1)
        add_url
        ;;
        2)
        remove_url
        ;;
        *)
        echo "请输入正确数字 [1-1]"
        ;;
    esac
}
add_url(){
    echo -e " 请输入要监听的种子页面:"
    read -e -p " (默认: 取消):" page
    [[ -z "${page}" ]] && echo -e "停止添加监控页面..." && exit 1
    domain=$(echo ${page} | awk -F'[/:]' '{print $4}')
    if [ -e cookies/$domain ]; then
        echo " 存有此页面Cookie, 下一步..."
    else
        echo " 请以Json格式输入此站点的Cookie:"
        read -e -p " (默认: 取消):" cookie
        [[ -z "${page}" ]] && echo -e "已取消..." && exit 1
        echo "$cookie">cookies/$domain
    fi
    echo -e " 请输入此页面筛选的种子最小体积, 单位为GB, -1 为不限制:"
    read -e -p " (默认: -1):" lower
    echo -e " 请输入此页面筛选的种子最大体积, 单位为GB, -1 为不限制:"
    read -e -p " (默认: -1):" higher
    echo -e " 请输入此页面下载的种子下载限速, 单位为MB/s, -1 为不限制:"
    read -e -p " (默认: -1):" download
    echo -e " 请输入此页面下载的种子上传限速, 单位为MB/s, -1 为不限制:"
    read -e -p " (默认: -1):" upload
    sed -i '/urls/a\'\\t\[\""$page"\","\"$lower"\","\"$higher"\","\"$download"\","\"$upload"\"\],'' config.json
}
remove_url(){
    echo -e " 请输入要删除的监听页面的 「序号」 :"
    read -e -p " (默认: 取消):" page_num
    [[ -z "${page_num}" ]] && echo -e "已取消..." && exit 1
    if [ $page_num -lt $len ]; then
        let page_num--
        sed -i "/${array[$[page_num*5]]}/d" config.json
        sed -i '/urls/a\'\\t\[\""$page"\","\"$lower"\","\"$higher"\","\"$download"\","\"$upload"\"\],'' config.json
    else
        echo "不存在这个页面, 取消 ..."  && exit 1
    fi
}
uninstall_boxhelper(){
    stop_boxhelper
    echo "正在无残留卸载 BoxHelper ..."
    rm -rf BoxHelper_QB
}

start_boxhelper(){
    echo "正在从后台启动 BoxHelper, 日志文件为 bh.log ..."
    nohup java -jar BoxHelper_QB/BoxHelper_QB.jar > bh.log 2>&1 &
}

stop_boxhelper(){
    echo "正在关闭 BoxHelper ..."
    [[ -z ${PID} ]] && echo -e " BoxHelper 没有运行" && exit 1
    kill -9 ${PID}
}
restart_boxhelper(){
    start_boxhelper
    stop_boxhelper
}

#菜单
menu(){
echo
echo " #############################################"
echo " # BoxHelper                                 #"
echo " # Github: https://github.com/SpereShelde    #"
echo " # Author: SpereShelde                       #"
echo " #############################################"

echo -e "
 ${Green_font_prefix} 1.${Font_color_suffix} 安装 BoxHelper
 ${Green_font_prefix} 2.${Font_color_suffix} 编辑 BoxHelper
 ${Green_font_prefix} 3.${Font_color_suffix} 卸载 BoxHelper
 ————————————————————————
 ${Green_font_prefix} 4.${Font_color_suffix} 启动 BoxHelper
 ${Green_font_prefix} 5.${Font_color_suffix} 停止 BoxHelper
 ${Green_font_prefix} 6.${Font_color_suffix} 重启 BoxHelper"

check_pid
if [[ ! -z "${PID}" ]]; then
	echo -e " 当前状态: BoxHelper ${Green_font_prefix}已启动${Font_color_suffix}"
else
	echo -e " 当前状态: BoxHelper ${Red_font_prefix}未启动${Font_color_suffix}"
fi
echo
read -e -p " 请输入数字 [1-6]:" num
case "$num" in
	1)
	install_boxhelper
	;;
	2)
	edit_boxhelper
	;;
	3)
	uninstall_boxhelper
	;;
	4)
	start_boxhelper
	;;
	5)
	stop_boxhelper
	;;
	6)
	restart_boxhelper
	;;
	*)
	echo "请输入正确数字 [0-10]"
	;;
esac
}

menu
