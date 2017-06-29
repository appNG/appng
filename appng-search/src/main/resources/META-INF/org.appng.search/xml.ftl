<?xml version="1.0" encoding="UTF-8"?>
<searchresults>
    <time>${time}</time>
    <query>${query}</query>
    <resultset chunk="${resultset.chunk}" chunkname="${resultset.chunkname}" chunksize="${resultset.chunksize}" nextchunk="${resultset.nextchunk}" previouschunk="${resultset.previouschunk}" firstchunk="${resultset.firstchunk}" lastchunk="${resultset.lastchunk}" hits="${resultset.hits}" maxhits="${resultset.maxhits}" hitRangeStart="${resultset.hitRangeStart}" hitRangeEnd="${resultset.hitRangeEnd}">
        <#list resultset.results as result>
        <result hit="${result.hit}">
            <#list result.fields as field>
            <${field.key}><![CDATA[${field.value}]]></${field.key}>
            </#list>
        </result>
        </#list>
    </resultset>
</searchresults>