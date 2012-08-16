$(document).ready(function() {
	var ie55 = (navigator.appName == "Microsoft Internet Explorer" && parseInt(navigator.appVersion) == 4 && navigator.appVersion.indexOf("MSIE 5.5") != -1);
	var ie6 = (navigator.appName == "Microsoft Internet Explorer" && parseInt(navigator.appVersion) == 4 && navigator.appVersion.indexOf("MSIE 6.0") != -1);
	if (ie55 || ie6) {
		$('.pin').css('background', "none")
		$('.shadow').css('background', "none")
		$('.paper').css('background', "none")
        $('.picframe').css('background', "none");
		$('.pin').css('filter', "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/public/css/img/pin.png', sizingMethod='scale')");
		$('.shadow').css('filter', "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/public/css/img/post_shadow.png', sizingMethod='scale')");
		$('.paper').css('filter', "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/public/css/img/paper.png', sizingMethod='scale')");
        $('.picframe').css('filter', "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/public/css/img/frame.png', sizingMethod='scale')");
	}
});
