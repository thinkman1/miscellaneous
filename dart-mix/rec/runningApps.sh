#!/bin/ksh
notrunning='Not Running'
deployclient=`ps -ef |grep lib/deployment-client |grep -v grep |awk {'print $2'}`
activemq=`ps -ef |grep apache-activemq |grep -v grep |awk {'print $2'}`
dart=`ps -ef |grep apps/dart/dart-portal/ |grep -v grep |awk {'print $2'}`
assemble=`ps -ef|grep dart-assemble |grep -v grep |awk {'print $2'}`
capture=`ps -ef|grep dart-capture |grep -v grep |awk {'print $2'}`
completion=`ps -ef|grep dart-completion |grep -v grep |awk {'print $2'}`
core_svc=`ps -ef| grep /apps/dart/core-services |grep -v grep |awk {'print $2'}`
eod=`ps -ef|grep dart-eod |grep -v grep |awk {'print $2'}`
extractsvc=`ps -ef|grep dart-extract-service |grep -v grep |awk {'print $2'}`
fiid=`ps -ef|grep dart-fiid |grep -v grep |awk {'print $2'}`
synch=`ps -ef|grep /apps/dart/file-sync-server |grep -v grep |awk {'print $2'}`
match=`ps -ef|grep dart-match |grep -v grep |awk {'print $2'}`
monitor=`ps -ef|grep dart-monitor |grep -v grep |awk {'print $2'}`
pcf_svr=`ps -ef|grep /apps/dart/pcf-server |grep -v grep |awk {'print $2'}`
pnc=`ps -ef|grep dart-pnc |grep -v grep |awk {'print $2'}`
property_web=`ps -ef|grep /apps/dart/property-web |grep -v grep |awk {'print $2'}`
reporting=`ps -ef|grep dart-reporting |grep -v grep |awk {'print $2'}`
services=`ps -ef|grep dart-services |grep -v grep |awk {'print $2'}`
testweb=`ps -ef|grep /apps/dart/dart-test-web |grep -v grep |awk {'print $2'}`
webservices=`ps -ef|grep dart-web-services |grep -v grep |awk {'print $2'}`
webviewer=`ps -ef|grep dart-web-viewer |grep -v grep |awk {'print $2'}`
xml=`ps -ef|grep dart-xml |grep -v grep |awk {'print $2'}`
event=`ps -ef|grep event-service |grep -v grep |awk {'print $2'}`
issueservice=`ps -ef|grep issue-service |grep -v grep |awk {'print $2'}`
reportweb=`ps -ef|grep dart-reports |grep -v grep |awk {'print $2'}`
issueweb=`ps -ef|grep issue-web |grep -v grep |awk {'print $2'}`
remediation=`ps -ef|grep remediation-web |grep -v grep |awk {'print $2'}`
HttpLanding=`ps -ef | grep http-landing-service | grep -v grep |awk {'print $2'}`
WebApache=`ps -ef | grep httpd | grep DART_WEBAPP.conf | grep -v grep |awk {'print $2'}`
PCname=`hostname |awk {'print $1'}`
echo "****************** DART PID FINDER*************************"
echo "Hostname: "$PCname
echo "Service Name                  : PID"
echo "___________________________________________________________"
echo "Deployment Client             : "${deployclient:-$notrunning}
echo "___________________________________________________________"
echo "ActiveMQ                      : "${activemq:-$notrunning}
echo "___________________________________________________________"
echo "DART-CAPTURE-SERVICES         : "${capture:-$notrunning}
echo "DART-COMPLETION-AGGREGATOR    : "${completion:-$notrunning}
echo "DART-EOD-SERVICE              : "${eod:-$notrunning}
echo "DART-EXTRACT-SERVICE          : "${extractsvc:-$notrunning}
echo "DART-FIID-EXTRACT-SERVICE     : "${fiid:-$notrunning}
echo "DART-MATCH-SERVICE            : "${match:-$notrunning}
echo "DART-MONITOR-SERVICE          : "${monitor:-$notrunning}
echo "DART-PNC-SERVICE              : "${pnc:-$notrunning}
echo "DART-REPORTING SERVICE        : "${reporting:-$notrunning}
echo "DART-SERVICES                 : "${services:-$notrunning}
echo "DART-XML-AGGREGATOR           : "${xml:-$notrunning}
echo "EVENT-SERVICE                 : "${event:-$notrunning}
echo "ISSUE-SERVICE                 : "${issueservice:-$notrunning}
echo "___________________________________________________________"
echo "Web Apps Apache Service       : "${WebApache:-$notrunning}
echo "___________________________________________________________"
echo "DART-PORTAL                   : "${dart:-$notrunning}
echo "DART-WEB-SERVICES             : "${webservices:-$notrunning}
echo "DART-WEB-VIEWER               : "${webviewer:-$notrunning}
echo "DART-ASSEMBLE-WEB             : "${assemble:-$notrunning}
echo "DART-REPORTS-WEB              : "${reportweb:-$notrunning}
echo "ISSUE-WEB                     : "${issueweb:-$notrunning}
echo "REMEDIATION-WEB               : "${remediation:-$notrunning}
echo "___________________________________________________________"
echo "FILE-SYNC-SERVICE             : "${synch:-$notrunning}
echo "CORE SERVICES                 : "${core_svc:-$notrunning}
echo "PROPERTY WEB                  : "${property_web:-$notrunning}
echo "___________________________________________________________"
echo "Http Landing Zone             : "${HttpLanding:-$notrunning}
echo "___________________________________________________________"
echo " Note:  Following services should only run in non-prod.    "
echo "PCF Server                    : "${pcf_svr:-$notrunning}
echo "Test Web                      : "${testweb:-$notrunning}
echo "********************* END OF SCRIPT ************************"