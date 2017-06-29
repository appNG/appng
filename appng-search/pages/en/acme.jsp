<%@page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@taglib uri="http://appng.org/tags" prefix="appNG"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head />
<body>
  <appNG:searchable index="false">
    <appNG:searchable index="true" field="title" visible="false">ACME</appNG:searchable>
    <appNG:searchable index="true" field="content">ACME</appNG:searchable>
  </appNG:searchable>
</body>
</html>
