<%@page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@taglib uri="http://appng.org/tags" prefix="appNG" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="de" lang="de">
<head>
	<style type="text/css">
		.error {color:red;background-color:yellow}
		label {display:block;margin-top:15px}
		div.error, input, select, textarea {width:220px}
	</style>  
</head>
<body>

	<appNG:form>
		<appNG:formData mode="not_submitted">
		<form action="" method="post" enctype="multipart/form-data" >

			<label>Name:</label>
			<appNG:formElement errorClass="error" rule="string" mandatory="true">
				<input type="text" name="name" value="lore ipsum"/>
			</appNG:formElement>

			<label>Number:</label>
			<appNG:formElement errorClass="error" rule="number" mandatory="true">
				<input type="text" name="number" value="4343"/>
			</appNG:formElement>

			<label>E-mail:</label>
			<appNG:formElement errorClass="error" rule="email" mandatory="true">
				<input type="text" name="email" value="mm@aiticon.de"/>
			</appNG:formElement>

			<label>Single File upload:</label>
			<appNG:formElement errorClass="error" rule="fileSizeMax('20KB')">
				<input type="file" name="file_1" />
			</appNG:formElement>

			<label>Multiple File upload:</label>
			<appNG:formElement errorClass="error" rule="fileCountMax(3)">
				<input type="file" name="file_2"  multiple="multiple"/>
			</appNG:formElement>

			<label>Check boxes:</label>
			<appNG:formElement>
				<input type="checkbox" name="checkbox1" value="a1" checked="checked"/>a1
			</appNG:formElement>
			<appNG:formElement>
				<input type="checkbox" name="checkbox1" value="a2"/>a2
			</appNG:formElement><br/>
			<appNG:formElement>
				<input type="checkbox" name="checkbox2" value="b" checked="checked"/>b
			</appNG:formElement>
			<appNG:formElement>
				<input type="checkbox" name="checkbox3" value="c"/>c
			</appNG:formElement>

			<label>Multiple options:</label>
			<appNG:formGroup name="select" mandatory="true" errorMessage="please make a selection" mandatoryMessage="please make a selection" errorElementId="select-error">
			<select name="select" multiple="multiple">
				<appNG:formElement>
					<option value="foo">foo</option>
				</appNG:formElement>
				<appNG:formElement>
					<option value="bar">bar</option>
				</appNG:formElement>
				<appNG:formElement>
					<option value="foobar" selected="selected">foobar</option>
				</appNG:formElement>
			</select><br/>
			<div id="select-error" class="error" />
			</appNG:formGroup>

			<label>Radio buttons:</label>
			<appNG:formElement>
				<input type="radio" name="radio_1" value="true" checked="checked"/>yes
			</appNG:formElement>
			<appNG:formElement>
				<input type="radio" name="radio_1" value="false" />no
			</appNG:formElement><br/>

			<appNG:formGroup name="select" mandatory="true" mandatoryMessage="please make a selection" errorElementId="radio-error" errorClass="error">
				<div id="radio-error" class="error" />
				<appNG:formElement>
					<input type="radio" name="radio_2" value="a" />a
				</appNG:formElement>
				<appNG:formElement>
					<input type="radio" name="radio_2" value="b" />b
				</appNG:formElement>
			</appNG:formGroup>

		 	<label>Textarea:</label>
			<appNG:formElement>
			 	<textarea name="textarea">lore ipsum</textarea>
			</appNG:formElement>

			<label>Captcha:</label>
			<img src="/service/manager/appng-webutils/webservice/captcha" /><br/>
			<!-- the result of the captcha ist stored in the variable SESSION['SESSION']['captcha'] -->
			<!-- where the first SESSION means the HTTP Session, ['SESSION'] the name of an attribute -->
			<!-- within the HTTP session. Since this attribute is also a map, you can use -->
			<!-- ['captcha'] to retrieve the result -->
			<appNG:formElement errorClass="error" rule="captcha(SESSION['SESSION']['captcha'])" mandatory="true" errorMessage="please enter the result" mandatoryMessage="please enter the result" errorElementId="captcha-error">
				<input type="text" name="captcha" />
				<div id="captcha-error" class="error"/>
			</appNG:formElement><br/>

			<input type="submit" />
		</form>
		</appNG:formData>
		<p/>
		<appNG:formConfirmation application="appng-webutils" method="emailProvider" mode="submitted">
			<!-- use #[param] to access the value of an input field-->
			<!-- the error message to display -->
			<appNG:param name="errorMessage">An error occurred while processing your input!</appNG:param>
			<!-- the freemarker template for the confirmation to display on the page -->
			<appNG:param name="confirmationTpl">/meta/tpl/email.ftl</appNG:param>
			<!-- the freemarker template for the  confirmation email send to the user (plaintext)-->
			<appNG:param name="emailTplText">/meta/tpl/email.ftl</appNG:param>
			<!-- the freemarker template for the  confirmation email send to the user (HTML)-->
			<appNG:param name="emailTplHtml">/meta/tpl/email.ftl</appNG:param>
			<!-- a comma-separated list of TO receivers -->
			<appNG:param name="receiver">#[email]</appNG:param>
			<!-- a comma-separated list of CC receivers -->
			<appNG:param name="receiverCC">#[email]</appNG:param>
			<!-- a comma-separated list of BCC receivers -->
			<appNG:param name="receiverBCC">#[email]</appNG:param>
			<!-- the sender's e-mail address -->
			<appNG:param name="sender">info@aiticon.de</appNG:param>
			<!-- set to true to disable sending and log the email instead-->
			<appNG:param name="sendDisabled">true</appNG:param>
			<!-- a comma-separated list of e-mail addresses to reply-to	 -->
			<appNG:param name="replyTo">#[email]</appNG:param>
			<!-- the subject for the e-mail -->
			<appNG:param name="subject">A testmail from the appNG Formframework</appNG:param>
			<!-- a comma-separated list of e-mail addresses to which the e-mail will be send (instead of the real receivers) -->
			<!--<appNG:param name="receiverDebug">#[email]</appNG:param>-->
			<!-- if set to true, the file attachments of the form will be added to the e-mails -->
			<appNG:param name="attachments">true</appNG:param>
			<!-- the message that is displayed then the e-mail was successfully sent (plus the content generated from 'confirmationTpl') -->
			<appNG:param name="content">
				<b>Thank you #[name] (#[email]) for your message!</b><br/>
			</appNG:param>
		</appNG:formConfirmation>
	</appNG:form>
</body> 
</html>
