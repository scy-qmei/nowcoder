$(function(){
	$("#publishBtn").click(publish);
});

//根据表单id获取表单项，val获取表单项的值


function publish() {
	//隐藏发布按钮
	$("#publishModal").modal("hide");
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title, "content":content},
		function (data) {
			data = $.parseJSON(data);
			//根据id获取文本框，text给文本框的内容赋值
			$("hintBody").text(data.msg);
			//显示提示框
			$("#hintModal").modal("show");
			//两秒之后关闭提示框，如果响应成功则刷新页面更新内容，否则不做任何操作
			setTimeout(function(){
				$("#hintModal").modal("hide");
				if (data.code == 0) {
					window.location.reload();
				}
			}, 2000);

		}
	)

}