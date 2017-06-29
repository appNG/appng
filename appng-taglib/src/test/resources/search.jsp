<%@page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@taglib uri="http://appng.org/tags" prefix="appNG"%>
<!DOCTYPE html>
<html>
<head />
<body>

	<form>
		<input type="text" name="q" value="<%=request.getParameter("q") == null ? "" : request.getParameter("q")%>"> <input
			type="hidden" name="ts" value="<%=java.lang.System.currentTimeMillis()%>" /> <input type="submit" />
	</form>

	<h2>JSON, with parts</h2>
	<textarea rows="20" cols="120">
<appNG:search parts="true" format="json" highlight="bold">
	<appNG:param name="dateFormat">yyyy-MM-dd</appNG:param>
	<appNG:param name="maxTextLength">150</appNG:param>	
	<appNG:searchPart application="acme-products" method="productSearchProvider" language="en"
				title="acme Products" fields="title,contents" analyzerClass="org.apache.lucene.analysis.en.EnglishAnalyzer">
		<appNG:param name="variantId">1</appNG:param>
		<appNG:param name="detailPage">/de/produkt/&lt;id>/&lt;name></appNG:param>
	</appNG:searchPart>
	<appNG:searchPart application="foobar-products" method="productSearchProvider" language="de" title="foobar Products"
				fields="" analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="productDetailPage">/en/product-detail</appNG:param>	
	</appNG:searchPart>
	<appNG:searchPart application="global" language="de" title="German Results" fields="title,contents"
				analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="excludeTypes">com.acme.products.domain.VariantProduct</appNG:param>
	</appNG:searchPart>
</appNG:search>
	</textarea>

	<h2>JSON, no parts</h2>
	<textarea rows="20" cols="120">
<appNG:search parts="false" format="json" highlight="bold">
	<appNG:param name="dateFormat">yyyy-MM-dd</appNG:param>
	<appNG:param name="maxTextLength">150</appNG:param>	
	<appNG:searchPart application="acme-products" method="productSearchProvider" language="en"
				title="acme Products" fields="title,contents" analyzerClass="org.apache.lucene.analysis.en.EnglishAnalyzer">
		<appNG:param name="variantId">1</appNG:param>
		<appNG:param name="detailPage">/de/produkt/&lt;id>/&lt;name></appNG:param>
	</appNG:searchPart>
	<appNG:searchPart application="foobar-products" method="productSearchProvider" language="de" title="foobar Products"
				fields="" analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="productDetailPage">/en/product-detail</appNG:param>	
	</appNG:searchPart>
	<appNG:searchPart application="global" language="de" title="German Results" fields="title,contents"
				analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="excludeTypes">com.acme.products.domain.VariantProduct</appNG:param>
	</appNG:searchPart>
</appNG:search>
	</textarea>

	<h2>XML</h2>
	<textarea rows="20" cols="120">
<appNG:search parts="false" format="xml" highlight="bold">
	<appNG:param name="dateFormat">yyyy-MM-dd</appNG:param>
	<appNG:param name="maxTextLength">150</appNG:param>	
	<appNG:searchPart application="acme-products" method="productSearchProvider" language="en"
				title="acme Products" fields="title,contents" analyzerClass="org.apache.lucene.analysis.en.EnglishAnalyzer">
		<appNG:param name="variantId">1</appNG:param>
		<appNG:param name="detailPage">/de/produkt/&lt;id>/&lt;name></appNG:param>
	</appNG:searchPart>
	<appNG:searchPart application="foobar-products" method="productSearchProvider" language="de" title="foobar Products"
				fields="" analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="productDetailPage">/en/product-detail</appNG:param>	
	</appNG:searchPart>
	<appNG:searchPart application="global" method="dummy" language="de" title="German Results" fields="title,contents"
				analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer">
		<appNG:param name="excludeTypes">com.acme.products.domain.VariantProduct</appNG:param>
	</appNG:searchPart>
</appNG:search>
	</textarea>

</body>
</html>
