$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	// var csrf = $("meta[name = '_csrf']").attr("content");
	// var csrfHeader = $("meta[name = '_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	xhr.setRequestHeader(csrfHeader, csrf);
	// });
	if($(btn).hasClass("btn-info")) {
		$.post(
			CONTEXT_PATH + "/follow",
			{
				"entityType":3,
				"entityId":$(btn).prev().val()
			},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		)
		// // 关注TA
		// $(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		$.post(
			CONTEXT_PATH + "/unfollow",
			{
				"entityType":3,
				"entityId":$(btn).prev().val()
			},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		)
		// // 取消关注
		// $(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}