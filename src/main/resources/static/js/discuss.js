//该方法是在页面加载完毕之后调用的！
$(function (){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});
function like(btn,entityType,entityId, entityUserId, postId) {
    //在发送异步请求之前，将csrf令牌加入请求中去
    // var csrf = $("meta[name = '_csrf']").attr("content");
    // var csrfHeader = $("meta[name = '_csrf_header']").attr("content");
    // $(document).ajaxSend(function (e, xhr, options) {
    //     xhr.setRequestHeader(csrfHeader, csrf);
    // });
    $.post(
        CONTEXT_PATH + "/like",
        {
            "entityType":entityType,
            "entityId":entityId,
            "entityUserId":entityUserId,
            "postId":postId
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==0?'赞':'已赞');
            } else {
                alert(data.msg);
            }
        }
    )
}

//置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {
            "postId":$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                //如果置顶成功，就更改按钮的状态为不可用
                $("#topBtn").attr("disabled","disabled");
            } else {
                alert(data.msg)
            }
        }
    )
}
//加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {
            "postId":$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#wonderfulBtn").attr("disabled","disabled");
            } else {
                alert(data.msg);
            }
        }
    )
}
//删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {
            "postId":$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                //如果删除成功，直接跳转到首页
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    )
}