$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    //使用ajax实现更加详细的异步请求的配置
    $.ajax({
        //请求url，从七牛云找
        url: "http://upload-z2.qiniup.com",
        method: "post",
        //不将数据转换为字符串，因为上传的是文件
        processData: false,
        //不让jquery设置内容类型，让浏览器自己区别是文件
        contentType: false,
        //设置提交的数据为表单的数据，【0】代表json格式
        data: new FormData($("#uploadForm")[0]),

        success: function(data) {
            //这里因为我们设置的响应数据的类型就是json，所以不用进行解析
            if(data && data.code === 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                        // window.location.reload();
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;
}