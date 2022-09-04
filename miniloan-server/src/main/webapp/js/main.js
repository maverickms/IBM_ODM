/*
 * Licensed Materials - Property of IBM                           
 * 5725-B69 5655-Y17                                              
 * Copyright IBM Corp. 2013, 2018. All Rights Reserved            
 * US Government Users Restricted Rights - Use, duplication or    
 * disclosure restricted by GSA ADP Schedule Contract with        
 * IBM Corp.                                                      
 */


$(document).ready(
		function() {
			hide_panels();

			// process the form
			$('form').submit(
					function(event) {
						// stop the form from submitting the normal way and
						// refreshing the page
						event.preventDefault();
						hide_panels();

						if ($("#use-odm").is(':checked')) {
							useODM = true
						} else {
							useODM = false
						}

						var formData = {
							'name' : $('input[name=name]').val(),
							'yearly-income' : parseInt($('input[name=yearly-income]').val()),
							'credit-score' : parseInt($('input[name=credit-score]').val()),
							'amount' : parseInt($('input[name=amount]').val()),
							'duration' : parseInt($('input[name=duration]').val()),
							'yearly-interest-rate' : parseFloat($('input[name=yearly-interest-rate]').val()),
							'use-odm' : useODM
						};

						// process the form
						var request = $.ajax({
							type : 'POST',
							url : 'validate',
							data : JSON.stringify(formData),
							dataType : 'json',
						});

						request.done(function(data) {
							console.log(data);
							$('#result-panel').css('visibility', 'visible');
							if (!data.error) {
								if (data.approved == true) {
									$('#result-panel').attr('class','panel panel-success');
									$('#result-header').text('Approved')
									$('#result-text').html(format_lines(data.messages))

								} else if (data.approved == false) {
									$('#result-panel').attr('class','panel panel-warning');
									$('#result-header').text('Rejected')
									$('#result-text').html(format_lines(data.messages))
								}
							} else {
								if (data.error == true) {
									$('#result-text').text(data.text)

								} else {
									$('#result-text').text("An unkonw error occured")
								}

								$('#result-panel').attr('class','panel panel-danger');
								$('#result-header').text('Error')
							}

							if (data.info && data.info.length > 0) {
								$('#info-panel').css('visibility', 'visible');
								$('#info-text').html(format_lines(data.info))
							}
						});

						request.fail(function(jqXHR, textStatus) {
							console.log("Request failed: " + textStatus);
							$('#result-panel').attr('class','panel panel-error');
							$('#result-header').text('Error')
							$('#result-text').text(textStatus)
						});

					});

		});

function format_lines(messages) {
	var t = messages.map(function(l) {
		return l.line;
	});
	return t.join("</br>");
}

function hide_panels() {
	$('#result-panel').css('visibility', 'hidden');
	$('#info-panel').css('visibility', 'hidden');
}