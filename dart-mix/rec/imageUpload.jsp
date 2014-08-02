<!-- Load Queue widget CSS and jQuery -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<link href='<c:url value="/css/jquery/plupload/jquery.plupload.queue.css"/>' 
	  rel="stylesheet" type="text/css" />

<!-- Third party script for BrowserPlus runtime (Google Gears included in Gears runtime now) -->
<script type="text/javascript" src="/dart-assemble-web/scripts/jquery/jquery-1.8.2.js"></script>
<script type="text/javascript" src="/dart-assemble-web/scripts/jquery/plupload/browserplus.js"></script>

<!-- Load plupload and all it's runtimes and finally the jQuery UI queue widget -->
<script type="text/javascript" src="/dart-assemble-web/scripts/jquery/plupload/plupload.full.js"></script>
<script type="text/javascript" src="/dart-assemble-web/scripts/jquery/plupload/jquery.plupload.queue.js"></script>

<script type="text/javascript">
// Convert divs to queue widgets when the DOM is ready
$(function() {
	$("#uploader").pluploadQueue({
		// General settings
		runtimes : 'silverlight,html5,flash,browserplus',
		url : "/dart-assemble-web/assemble/processUploadedImage.htm?SK_JPMC_XSRF=${SK_JPMC_XSRF}",
		max_file_size : '1gb',
		
		unique_names : true,
		multipart: true,
		
		
		file_data_name: "imgFile",

		// Resize images on clientside if we can
		//resize : {width : 320, height : 240, quality : 90},

		// Specify what files to browse for
		filters : [
			{title : "Jpg files", extensions : "jpg"}
		],

		// Flash settings
		flash_swf_url : '/dart-assemble-web/scripts/jquery/plupload/plupload.flash.swf',

		// Silverlight settings
		silverlight_xap_url : '/dart-assemble-web/scripts/jquery/plupload/plupload.silverlight.xap',
		
        // Post init events, bound after the internal events  

		init : {
		
			FileUploaded: function(up, file, info) {  
				// Called when a file has finished uploading  
	
/* 				var response = info.response.split("|");				
				var responseCode = info.response.split("|")[0];
				if(responseCode != "200") {
					file.status = plupload.FAILED;
					if($("#fileDropResultsContainer").css('display') == 'none') {
						$("#fileDropResultsContainer").css('display', 'block');
					}
					$("#fileDropResults").append('<p style="color:red;">File:' + file.name 
												+ " cannot be dropped. (Status Code: " + responseCode 
												+ "; Status Description:" + response[1] + ")</p>");

				} */
			},
			
			UploadComplete: function(up, files) {
				parent.$(parent.document).trigger("updateImagesEvent");
			}
		}

		
	});

	// Client side form validation
	$('form').submit(function(e) {
        var uploader = $('#uploader').plupload('getUploader');

        // Files in queue upload them first
        if (uploader.files.length > 0) {
            // When all files are uploaded submit form
            uploader.bind('StateChanged', function() {
                if (uploader.files.length === (uploader.total.uploaded + uploader.total.failed)) {
                    $('form')[0].submit();
                }
            });
                
            uploader.start();
        } else {
            alert('You must at least upload one file.');
        }

        return false;
    });
	
});

</script>

<form>
	<div id="uploader">
		<p>
			<img src='<c:url value="/images/waitingImg.jpg" />' height="17" width="17" style="border-style: none" />
			Page loading...
		</p>
	</div>
</form>

<div id="fileDropResultsContainer" style="background-color:#ffffff; padding:8px; display:none;">
	<div id="fileDropResults" style="background-color:#cccccc; padding:8px;">
	</div>
</div>
			
