$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");
	var toName = $("#recipient-name").val();
	var toContent = $("#message-text").val();
	// var csrf = $("meta[name = '_csrf']").attr("content");
	// var csrfHeader = $("meta[name = '_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	xhr.setRequestHeader(csrfHeader, csrf);
	// });
	$.post(
		CONTEXT_PATH + "/message/add",
		{
			"toName":toName,
			"content":toContent
		},
		function (data) {
			data = $.parseJSON(data);
			$("#hintBody").text(data.msg);
		}
	)
	$("#hintModal").modal("show");
	setTimeout(function(){
		$("#hintModal").modal("hide");
		location.reload();
	}, 2000);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}