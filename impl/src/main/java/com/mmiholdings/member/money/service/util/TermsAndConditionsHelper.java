package com.mmiholdings.member.money.service.util;

public class TermsAndConditionsHelper {

    public static String getEmailTermsAndConditions(String receiversName) {
            return "<!DOCTYPE html><html>\n" +
                    "<body>\n" +
                    "<table>\n" +
                    "    <tr>\n" +
                    "        <td style=\"padding: 3.5em\">\n" +
                    "            <p style=\"margin-bottom:10.0pt; line-height:115%\">\n" +
                    "\t\t\t<span style=\"font-size:12.0pt; line-height:115%; font-family:Arial,sans-serif\">\n" +
                    "\t\t\t\tHi " + receiversName + ",\n" +
                    "\t\t\t</span>\n" +
                    "            </p>\n" +
                    "\n" +
                    "            <p style=\"margin-bottom:10.0pt; line-height:115%\">\n" +
                    "\t\t\t<span style=\"font-size:12.0pt; line-height:115%; font-family:Arial,sans-serif\">\n" +
                    "\t\t\t\tThank you for your Multiply Visa ® Card application.\n" +
                    "\t\t\t</span>\n" +
                    "            </p>\n" +
                    "\n" +
                    "            <p style=\"margin-bottom:10.0pt; line-height:115%\">\n" +
                    "\t\t\t<span style=\"font-size:12.0pt; line-height:115%; font-family:Arial,sans-serif\">\n" +
                    "\t        \tPlease find attached the Terms and Conditions of the card.\n" +
                    "\t        </span>\n" +
                    "            </p>\n" +
                    "            <p style=\"margin-bottom:10.0pt; line-height:115%\">\n" +
                    "\t\t\t<span style=\"font-size:12.0pt; line-height:115%; font-family:Arial,sans-serif\">\n" +
                    "\t        \tRegards\n" +
                    "        \t\t<br/>\n" +
                    "\t        \tYour Multiply Money Team\n" +
                    "    \t\t</span>\n" +
                    "            </p>\n" +
                    "\n" +
                    "            <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"671\"\n" +
                    "                   style=\"width:503.25pt;margin-left:4em \">\n" +
                    "                <tbody>\n" +
                    "                <tr style=\"margin-right:12.0pt\">\n" +
                    "                    <td width=\"456\" style=\"width:4.75in; padding:0in 0in 0in 0in\">\n" +
                    "                        <p style=\"margin-bottom:12.0pt\"><b><span\n" +
                    "                                style=\"font-size:11.0pt; font-family:&quot;Calibri&quot;,&quot;sans-serif&quot;; color:#D71921\">Money Management team</span></b>\n" +
                    "                        </p>\n" +
                    "                        <p>\n" +
                    "                            <span style=\"font-size:11.0pt; font-family:&quot;Calibri&quot;,&quot;sans-serif&quot;; color:#424242\">\n" +
                    "                                <b>Tel:</b> 0860 111 183\n" +
                    "                                <br>\n" +
                    "                                <b>International number:</b> +27 (0)12 675 3833\n" +
                    "                                <br><br>\n" +
                    "                                268 West Avenue, Centurion, 0157,&nbsp;PO Box 7400, Centurion, 0046&nbsp;<br>\n" +
                    "                                <b>Email:</b>\n" +
                    "                            </span>\n" +
                    "                            <span style=\"font-size:11.0pt; font-family:&quot;Calibri&quot;,&quot;sans-serif&quot;; color:#424242\">\n" +
                    "                                <b><a href=\"URL=mailto:multiplyvisacard@multiply.co.za\">multiplyvisacard@multiply.co.za</a></b>\n" +
                    "                                <br>\n" +
                    "                                <b>Website:</b> <a href=\"URL=http://www.multiply.co.za/multiplyvisacard\">www.multiply.co.za/multiplyvisacard</a>\n" +
                    "                            </span>\n" +
                    "                        </p>\n" +
                    "                    </td>\n" +
                    "                </tr>\n" +
                    "                </tbody>\n" +
                    "            </table>\n" +
                    "    </tr>\n" +
                    "</table>\n" +
                    "</body>\n" +
                    "</html>";
    }
}
