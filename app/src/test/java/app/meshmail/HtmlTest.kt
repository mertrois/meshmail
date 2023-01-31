package app.meshmail

import app.meshmail.service.sanitizeText
import org.junit.Test

class HtmlTest {
    @Test
    fun testSanitize() {
        println(sanitizeText(html_body))
    }
}

var html_body = """
   <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns=3D"http://www.w3.org/1999/xhtml" xml:lang=3D"en" lang=3D"en">
<head>
<title>    Find affordable coverage by Jan. 15 at HealthCare.gov
</title>
    <meta content=3D"text/html; charset=3DUTF-8">
<meta name=3D"viewport" content=3D"initial-scale=3D1.0">
<meta name=3D"format-detection" content=3D"telephone=3Dno">
<style type=3D"text/css">
    /*start reset css*/

div { line-height: 1; }
body, table, td, p, a, li, blockquote { -webkit-text-size-adjust: 100%; -ms=
-text-size-adjust: 100%; }
body { -webkit-text-size-adjust: none; -ms-text-size-adjust: none; }
table { border-spacing: 0; }
table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
img { -ms-interpolation-mode: bicubic; }
body { margin: 0; padding: 0; }
img { border: none !important; height: auto; line-height: 1; outline: none;=
 text-decoration: none; }
table td { border-collapse: collapse !important; }

.preheader { display: none !important; visibility: hidden; opacity: 0; colo=
r: transparent; height: 0; width: 0; }
  /*end reset css*/
.gd_combo_text_cell { padding: 10px !important; }
.gd_combo_image_cell { padding: 10px !important; }

@media only screen and (max-device-width: 480px) {
    table#yahoo, table#yahoo table { position: relative; }
    table#yahoo .govd_header { max-width: 480px !important; }
    table#yahoo .govd_template_image { height: auto !important; max-width: =
480px !important; width: 100% !important; margin:0 !important; }
  table#yahoo .govd_content_container .govd_image_display { margin-left:0 !=
important; }
    table#yahoo .container-fill { max-width: 480px !important; }

  .main-table, .main-footer, .mobile-footer, .mobile-tagline { width:100% !=
important; min-width:100% !important; max-width:480px !important; }
  table#yahoo .tablecell { width:100% !important; display: block !important=
; }
  table#yahoo p, table#yahoo .gd_p, table#yahoo li { font-size:13pt !import=
ant; }
  table#yahoo h1 { font-size:18pt !important; }
  table#yahoo h2 { font-size:16pt !important; }
  table#yahoo h3 { font-size:14pt !important; }

  .main-footer, .main-footer table { width:100% !important; max-width:480px=
 !important; }

.main-footer .main-footer-left { width:100% !important; }

.main-footer .main-footer-right { width:0 !important; }

.main-footer img.hide, .main-footer td.hide { display: none !important; wid=
th: 0; height: auto; }

.main-footer .hide { display: none !important; }

  .mobile-hide { display:none !important; }

table#yahoo #sub-headline-body table.gd_combo_table tr.gd_combo_first_image=
 td.gd_combo_image_cell {width:21% !important;}
table#yahoo #sub-headline-body table.gd_combo_table tr.gd_combo_first_image=
 td.gd_combo_text_cell {width:79% !important; padding:5px 0 5px 0 !importan=
t;}

  table#yahoo #main-graphic h2 {margin-bottom:0 !important;}

  table#yahoo #sub-headline-body table.gd_combo_table .govd_template_image =
{margin:13px 0 5px 0 !important;}
  table#yahoo #sub-headline-body table.gd_combo_table tr.gd_combo_first_ima=
ge td.gd_combo_image_cell .govd_template_image {margin:0 !important; !impor=
tant;}


}

  </style>

</head>
<body style=3D"line-height:140%;margin:0;padding:0;margin:0;padding:0;width=
:100% !important;">
<table bgcolor=3D'#ffffff' border=3D'0' cellpadding=3D'0' cellspacing=3D'0'=
 style=3D'width: 100%;'>
<tbody>
<tr bgcolor=3D'#ffffff'>
<td align=3D'center' style=3D'background-color: #ffffff; padding: 0px; font=
-size:1px; font-family:&#39;Helvetica Neue&#39;, Helvetica, Arial, sans-ser=
if; color:#ffffff' valign=3D'top'>
Visit HealthCare.gov to see if you qualify for financial help.
</td>
</tr>
</tbody>
</table>


<!--[if (gte mso 9)|(IE)]>
  <table style=3D"display:none"><tr><td><a name=3D"gd_top" id=3D"gd_top"></=
a></td></tr></table>
<![endif]-->
<a name=3D"gd_top" id=3D"gd_top"></a>






  <table width=3D"100%" cellpadding=3D"0" cellspacing=3D"0" border=3D"0" al=
ign=3D"center" class=3D"gd_tbl_wrap"><tr><td style=3D"background-color: #f0=
f0f1; margin: 0; padding: 0; width: 100% !important" bgcolor=3D"#f0f0f1"><t=
able class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0" =
cellspacing=3D"0" id=3D"yahoo" style=3D"border-collapse: collapse; margin: =
0 auto;">

<tr valign=3D"top" class=3D"mobile-hide">
<td valign=3D"top" style=3D"padding: 10px 0 20px;">
<table class=3D"main-table" width=3D"600" align=3D"left" cellpadding=3D"0" =
cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margin:=
 0 auto;">
<tr valign=3D"top">

<td width=3D"80%" valign=3D"bottom" class=3D"tablecell" align=3D"left">
<table width=3D"100%" align=3D"left" cellpadding=3D"0" cellspacing=3D"0" va=
lign=3D"bottom" style=3D"border-collapse: collapse; table-layout: auto; mar=
gin: 0 auto;">
<tr valign=3D"bottom">
<td id=3D"preheader-left" width=3D"100%" data-govdstyle=3D"all" data-govdti=
tle=3D"Preheader Left" valign=3D"bottom" style=3D"padding: 0px 5px;">

</td>
</tr>
</table>
</td>

<td width=3D"20%" valign=3D"bottom" class=3D"tablecell" align=3D"right">
<table width=3D"100%" align=3D"right" cellpadding=3D"0" cellspacing=3D"0" v=
align=3D"bottom" style=3D"border-collapse: collapse; table-layout: auto; ma=
rgin: 0 auto;">
<tr valign=3D"bottom">
<td id=3D"preheader-right" width=3D"100%" valign=3D"bottom" align=3D"right"=
>
<p class=3D"gd_p" style=3D"text-align: right; mso-line-height-rule: exactly=
; line-height: 140%; color: #666; font-family: arial,helvetica,sans-serif; =
font-size: 11px; margin: 0; padding: 0;" align=3D"left"><a href=3D"https://=
lnks.gd/l/eyJhbGcijY291bnRzL1VTQ01TSElNL2J1bGxldGlucy8zND=
BlNmZjIn0.Wja34zpUSbb9WM5p-rXfP73ixsFCOzmc-LH5OOMjLxk/s/440305174/br/151948=
143821-l" style=3D"color: #1473cb; text-decoration: none; -ms-word-break: b=
reak-all; word-break: break-word; -webkit-hyphens: none; -moz-hyphens: none=
; hyphens: none;"><span style=3D"text-decoration: underline; color: #666; m=
so-line-height-rule: exactly; line-height: 140%;">View in browser</span></a=
></p>
</td>
</tr>
</table>
</td>

</tr>
</table>
</td>
</tr>

<tr valign=3D"top">
<td valign=3D"top" style=3D"border-top-width: 5px; border-top-color: #0d76a=
d; border-top-style: solid;">
  <table class=3D"main-table" width=3D"600" align=3D"left" cellpadding=3D"0=
" cellspacing=3D"0" bgcolor=3D"#FFFFFF" style=3D"background-color: #fff; wi=
dth: 600px; border-collapse: collapse; margin: 0 auto;">
<tr valign=3D"top">

<td width=3D"27%" valign=3D"top" class=3D"mobile-hide" align=3D"left">
<table width=3D"100%" align=3D"left" cellpadding=3D"0" cellspacing=3D"0" va=
lign=3D"top" style=3D"border-collapse: collapse; table-layout: auto; margin=
: 0 auto;">
<tr valign=3D"top">
<td id=3D"header-left" width=3D"100%">
<a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1NiJ9.eyJidWxsZXRpbl9saW5rX2lkI=
joxMDEsInVyaSI6ImJwMjp2RuYSZ1dG1fY29udGVudD1lbmdsaXNoJnV0bV9tZWRpdW09ZW=
1haWwmdXRtX3NvdXJjZT1nxpdmVyeSJ9.n0NX6WdRCNQunGFAJNYptk4ZkkDaDaZMXODG=
9XRzNPk/s/440305174/br/151948143821-l" target=3D"_blank" rel=3D"noopener no=
referrer" style=3D"color: #0000EE; text-decoration: underline; -ms-word-bre=
ak: break-all; word-break: break-word; -webkit-hyphens: none; -moz-hyphens:=
 none; hyphens: none;"><img class=3D"govd_template_image" src=3D"https://co=
ntent.govdelivery.com/attachments/fancy_images/USCMSHIM/2015/11/663147/whit=
espace_original.jpg" style=3D"width: 162px; height: auto !important; max-he=
ight: auto !important; border: none;" alt=3D"HealthCare.gov with White back=
ground hcgov" width=3D"162" height=3D"58"></a>
</td>
</tr>
</table>
</td>

<td width=3D"46%" valign=3D"top" class=3D"tablecell" align=3D"left">
<table width=3D"100%" align=3D"left" cellpadding=3D"0" cellspacing=3D"0" va=
lign=3D"top" style=3D"border-collapse: collapse; table-layout: auto; margin=
: 0 auto;">
<tr valign=3D"top">
<td id=3D"main-header" width=3D"100%">
<table width=3D"100%" border=3D"0" cellspacing=3D"0" cellpadding=3D"0" styl=
e=3D"border-collapse: collapse; table-layout: auto; margin: 0 auto;"><tr><t=
d align=3D"center"><a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1NiJ9.eyJidW=
xsZXRpbl9saW5rX2lkIjoxMDIsInVyaSI6ImJwMjpjbGljayIsImJ1bGxldGluX2lkIjoiMjAyM=
zAxMDkuNjk0NDE1MjEiLCJ1cmbr/151948143821-l" target=3D"_blank"=
 rel=3D"noopener noreferrer" style=3D"color: #0000EE; text-decoration: unde=
rline; -ms-word-break: break-all; word-break: break-word; -webkit-hyphens: =
none; -moz-hyphens: none; hyphens: none;"><img class=3D"govd_template_image=
" src=3D"https://content.govdelivery.com/attachments/fancy_images/USCMSHIM/=
2015/11/663146/healthcaredotgov-b_original.jpg" style=3D"width: 276px; heig=
ht: auto !important; max-height: auto !important; border: none;" alt=3D"Hea=
lthCare.gov with White background, Blue and Black letters hcgov" width=3D"2=
76" height=3D"58"></a></td></tr></table>
</td>
</tr>
</table>
</td>

<td width=3D"27%" valign=3D"top" class=3D"mobile-hide" align=3D"left">
<table width=3D"100%" align=3D"left" cellpadding=3D"0" cellspacing=3D"0" va=
lign=3D"top" style=3D"border-collapse: collapse; table-layout: auto; margin=
: 0 auto;">
<tr valign=3D"top">
<td id=3D"header-right" width=3D"100%">
<a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1NiJ9.eyJidWxsZXRpbl9saW5rX2lkI=
joxMDMsInVyaSI6ImJwMxpdmVyeSJ9.peAXfGGmsxrkHInRDBbUUL0Aghok5xWdvTOE=
EPY1suU/s/440305174/br/151948143821-l" target=3D"_blank" rel=3D"noopener no=
referrer" style=3D"color: #0000EE; text-decoration: underline; -ms-word-bre=
ak: break-all; word-break: break-word; -webkit-hyphens: none; -moz-hyphens:=
 none; hyphens: none;"><img class=3D"govd_template_image" src=3D"https://co=
ntent.govdelivery.com/attachments/fancy_images/USCMSHIM/2015/11/663147/whit=
espace_original.jpg" style=3D"width: 162px; height: auto !important; max-he=
ight: auto !important; border: none;" alt=3D"HealthCare.gov with White back=
ground hcgov" width=3D"162" height=3D"58"></a>
</td>
</tr>
</table>
</td>

</tr>
</table>
</td>
</tr>

<tr>
<td style=3D"background-color: #04689b; padding-bottom: 6px;" bgcolor=3D"#0=
4689b">
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"main-headline" width=3D"100%" data-govdstyle=3D"all" data-govdtit=
le=3D"Main Headline" style=3D"background-color: #0d76ad; padding: 20px;" bg=
color=3D"#0d76ad">
<h1 style=3D"text-align: center; mso-line-height-rule: exactly; line-height=
: 1.1; font-weight: normal; color: #ffffff; font-family: arial, helvetica, =
sans-serif; font-size: 35px; margin: 0; padding: 0;" align=3D"center">Don=
=E2=80=99t wait any longer! Find coverage by Jan. 15</h1>
</td>
</tr>
</table>
</td>
</tr>

<tr>
<td>
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"main-graphic" width=3D"100%" data-govdstyle=3D"all" data-govdtitl=
e=3D"Main Graphic" style=3D"background-color: #ffffff; padding: 20px 10px 0=
px;" bgcolor=3D"#ffffff">
<table width=3D"100%" border=3D"0" cellspacing=3D"0" cellpadding=3D"0" styl=
e=3D"border-collapse: collapse; table-layout: auto; margin: 0 auto;"><tr><t=
d align=3D"center"><a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1NiJ9.eyJidW=
xsZXRpbl9saW5rX2lkIjoxMDQsInVyaSI6ImJwMjpjbGljayIsImJ1bGxldGluX2lkIjoiMjAyM=
zAxMDkuNjk0NDE1MjEiLC/br/151948143821-l" target=3D"_blank"=
 rel=3D"noopener noreferrer" style=3D"color: #0000EE; text-decoration: unde=
rline; -ms-word-break: break-all; word-break: break-word; -webkit-hyphens: =
none; -moz-hyphens: none; hyphens: none;"><img class=3D"govd_template_image=
" src=3D"https://content.govdelivery.com/attachments/fancy_images/USCMSHIM/=
2016/01/715449/urgent-january-15-deadline_original.png" style=3D"width: 470=
px; height: auto !important; max-height: auto !important; border: none;" al=
t=3D"Urgent January 15 Deadline" width=3D"470" height=3D"45"></a></td></tr>=
</table>
</td>
</tr>
</table>
</td>
</tr>

<tr>
<td>
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"sub-headline-body" width=3D"100%" data-govdstyle=3D"all" data-gov=
dtitle=3D"Sub Headline - body" style=3D"background-color: #FFFFFF; padding:=
 20px 20px 10px;" bgcolor=3D"#FFFFFF">
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left">Luigi</p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left">=C2=A0</p>
<p class=3D"gd_p" style=3D"text-align: left; mso-line-height-rule: exactly;=
 line-height: 140%; color: #333333; font-family: arial, helvetica, sans-ser=
if; font-size: 17px; margin: 0; padding: 0;" align=3D"left">Our records sho=
w you still need to apply for 2023 health coverage. <strong>Don=E2=80=99t w=
ait any longer - January 15 is the final day to enroll for coverage startin=
g February 1</strong>! Return to <a href=3D"https://lnks.gd/l/eyJhU=
zI1NiJ9.eyJidWxsZXRpbl1mWXnYEXbJk/s/440305174/br/151948143821-l" sty=
le=3D"color: #0000EE; text-decoration: underline; -ms-word-break: break-all=
; word-break: break-word; -webkit-hyphens: none; -moz-hyphens: none; hyphen=
s: none;">HealthCare.gov</a> and see what savings you qualify for and pick =
the right plan for you. </p>
<p class=3D"gd_p" style=3D"text-align: left; mso-line-height-rule: exactly;=
 line-height: 140%; color: #333333; font-family: arial, helvetica, sans-ser=
if; font-size: 17px; margin: 0; padding: 0;" align=3D"left">=C2=A0</p>
</td>
</tr>
</table>
</td>
</tr>

<tr>
<td>
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"call-to-action" width=3D"100%" data-govdstyle=3D"all" data-govdti=
tle=3D"Call to Action" style=3D"background-color: #FFFFFF; padding: 0px 20p=
x 5px;" bgcolor=3D"#FFFFFF">
<table width=3D"100%" border=3D"0" cellspacing=3D"0" cellpadding=3D"0" styl=
e=3D"border-collapse: collapse; table-layout: auto; margin: 0 auto;"><tr><t=
d align=3D"center"><a href=3D"https://lnks.gd/l/l7jDm=
4xKh7aVcGyeSxDpk0kFFwmB8MU/s/440305174/br/151948143821-l" target=3D"_blank"=
 rel=3D"noopener noreferrer" style=3D"color: #0000EE; text-decoration: unde=
rline; -ms-word-break: break-all; word-break: break-word; -webkit-hyphens: =
none; -moz-hyphens: none; hyphens: none;"><img class=3D"govd_template_image=
" src=3D"https://content.govdelivery.com/attachments/fancy_images/USCMSHIM/=
2022/05/5855583/log-in-case_original.png" style=3D"width: 305px; height: au=
to !important; max-height: auto !important; border: none;" alt=3D"log in" w=
idth=3D"305" height=3D"49"></a></td></tr></table>
</td>
</tr>
</table>
</td>
</tr>

<tr>
<td>
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"bottom-body" width=3D"100%" data-govdstyle=3D"all" data-govdtitle=
=3D"Bottom Body" style=3D"background-color: #FFFFFF; padding: 0px 20px 20px=
;" bgcolor=3D"#FFFFFF">
<p class=3D"gd_p" style=3D"text-align: left; mso-line-height-rule: exactly;=
 line-height: 140%; color: #333333; font-family: arial, helvetica, sans-ser=
if; font-size: 17px; margin: 0; padding: 0;" align=3D"left">=C2=A0</p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left">There=E2=80=99s more than one =
way to enroll in Marketplace coverage.<strong> If you would like assistance=
 from an agent or broker, or want to enroll through one of our certified en=
rollment partners, learn more </strong><a href=3D"https://lnks.gd/l/nkifQ.QFCw0zLW9wVUuU7MnDjzJM6Dw0H3VXUhwSd4PS=
a95z0/s/440305174/br/151948143821-l" style=3D"color: #0000EE; text-decorati=
on: underline; -ms-word-break: break-all; word-break: break-word; -webkit-h=
yphens: none; -moz-hyphens: none; hyphens: none;"><strong>here</strong></a>=
<strong>.=C2=A0</strong></p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left">=C2=A0</p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left"><strong>Remember to enroll by =
January 15 for coverage starting February 1.=C2=A0</strong></p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left">=C2=A0</p>
<p style=3D"mso-line-height-rule: exactly; line-height: 140%; color: #33333=
3; font-family: arial, helvetica, sans-serif; font-size: 17px; text-align: =
left; margin: 0; padding: 0;" align=3D"left"><em>The </em><a href=3D"https:=
//lnks.gd/l/ixEbWKF_24vcN6mKrOTZCLNTtI-cA/s/440305174/br/15=
1948143821-l" style=3D"color: #0000EE; text-decoration: underline; -ms-word=
-break: break-all; word-break: break-word; -webkit-hyphens: none; -moz-hyph=
ens: none; hyphens: none;"><em>HealthCare.gov</em></a><em> Team</em></p>
</td>
</tr>
</table>
</td>
</tr>

<tr>
<td>
<table class=3D"main-table" width=3D"600" align=3D"center" cellpadding=3D"0=
" cellspacing=3D"0" style=3D"width: 600px; border-collapse: collapse; margi=
n: 0 auto;">
<tr>
<td id=3D"main-footer" width=3D"100%" data-govdstyle=3D"all" data-govdtitle=
=3D"Main Footer" style=3D"background-color: #0d76ad; padding: 20px;" bgcolo=
r=3D"#0d76ad">

</td>
</tr>
</table>
</td>
</tr>

</table></td></tr></table>



<div id=3D"mail_footer">
    <table class=3D"main-footer" style=3D"border-collapse: collapse; margin=
: 0 auto; width: 100%; font-size: 1em;" border=3D"0" cellspacing=3D"0" cell=
padding=3D"0" align=3D"center" bgcolor=3D"#f0f0f1">
<tbody>
<tr valign=3D"top">
<td style=3D"border-collapse: collapse;" valign=3D"top">
<table style=3D"border-collapse: collapse; margin: 0 auto; width: 600px;" b=
order=3D"0" cellspacing=3D"0" cellpadding=3D"0" align=3D"center">
<tbody>
<tr valign=3D"top">
<td style=3D"padding: 30px 0;" align=3D"center" valign=3D"top">
<table style=3D"border-collapse: collapse; margin: 0 auto; width: 600px;" b=
order=3D"0" cellspacing=3D"0" cellpadding=3D"0" align=3D"left">
<tbody>
<tr valign=3D"top">
<td class=3D"main-footer-right" style=3D"border-collapse: collapse;" align=
=3D"center" valign=3D"top" width=3D"70"><img class=3D"hide" src=3D"https://=
content.govdelivery.com/attachments/fancy_images/USDHSFEMA/2015/09/613520/l=
ogo-dhhs-trans-fiftyfive_original.png" width=3D"60" height=3D"56" style=3D"=
display: block; border-width: 0; border: 0;"></td>
<td class=3D"main-footer-left" style=3D"border-collapse: collapse; padding:=
 0 5px 0 5px; text-align: left;" valign=3D"top">
<p style=3D"font-family: Arial, Helvetica, sans-serif; font-size: 0.7em; co=
lor: #666666; mso-line-height-rule: exactly; line-height: 120%;">This messa=
ge is paid for by the U.S. Department of Health and Human Services. It was =
created and distributed by the Centers for Medicare &amp; Medicaid Services=
. You're receiving this message because you signed up for email updates fro=
m the HealthCare.gov Team. You can <a href=3D"lBf45Z7ZUc/s/440305174/br/151=
948143821-l" title=3D"update your preferences">update your preferences</a>,=
 <a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1hLoQr2pIRpZsWzVzWKIkRIv-1qzHT=
E/s/440305174/br/151948143821-l" title=3D"receive fewer emails">receive few=
er emails</a>=C2=A0or <a href=3D"https://lnks.YmxpYy5nb3ZkZWxpdmVyeS5jb20vY=
WNjb3VudHMvVVNDTVNISU0vc3Vic2NyaWJlci9lZGl0I3RhYjMifQ.llXBcsI8iv_x9uf8cfuWh=
VNELASicF4fkd7OKu32Q1I/s/440305174/br/151948143821-l" title=3D"pause emails=
">pause emails</a>=C2=A0until the next Open Enrollment period, or use our <=
a href=3D"https://lnks.gd/l/eyJhbGciOiJIUzI1NiRzL1VTQ01TSElNL3N1YnNjcmliZXI=
vb25lX2NsaWNrX3Vuc3Vic2NyaWJlIn0._-dMFPcdURPBvKJol0I9HSklwcYrVcGQK_TJ3cBUP7=
M/s/440305174/br/151948143821-l?verification=3D5.e1a22a7eb420ccd387242d3c2b=
82d754&destination=%40gmail.com">1-click unsubscribe</a>=C2=A0to st=
op receiving messages from the HealthCare.gov Team. Please contact support@=
subscriptions.cms.hhs.gov if you have questions or problems with your subsc=
riptions.</p>
</td>
</tr>
</tbody>
</table>
</td>
</tr>
</tbody>
</table>
</td>
</tr>
</tbody>
</table>
<p>=C2=A0</p>

</div>
<div id=3D"tagline">
    <hr>
<table style=3D"width: 100%;" border=3D"0" cellspacing=3D"0" cellpadding=3D=
"0">
<tbody>
<tr>
<td style=3D"color: #757575; font-size: 10px; font-family: Arial;" width=3D=
"89%">This email was sent to  using GovDelivery Communicati=
ons Cloud on behalf of the Health Insurance Marketplace =C2=B7 <span>7500 S=
ecurity Boulevard =C2=B7 Baltimore MD 21244</span>=C2=A0=C2=B7 1-800-318-25=
96</td>
</tr>
</tbody>
</table>

</div>

<IMG SRC=3D"https://links.govdelivery.com/track?enid=3DZWFzPTEmYnVsbGV0aW5y=
ZWNpcGllbnRpZD0xNTE5NDgxNDM4MjEtbCZzdWJzY3JpYmVyaWQ9NDQwMzA1MTc0Jm1zaWQ9JmF=
1aWQ9Jm1haWxpbmdpZD0yMDIzMDEwOS42OTQ0MTUyMSZtZXNzYWdlaWQ9TURCLVBSRC1CVUwtMj=
AyMzAxMDkuNjk0NDE1MjEmZGF0YWJhc2VpZD0xMDAxJnR5cGU9b3BlbiZzZXJpYWw9MTY4NjUxM=
zEmZW1haWxpZD1sdWtlcmxAZ21haWwuY29tJnVzZXJpZD1sdWtlcmxAZ21haWwuY29tJnRhcmdl=
dGlkPSZmbD0mbXZpZD0mZXh0cmE9JiYm" WIDTH=3D"1" HEIGHT=3D"1" STYLE=3D"border-=
width:0; border-style:hidden;" ALT=3D""/></body>
</html>

""".trimIndent()