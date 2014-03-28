Badger
======

Application for creating unique event badges for attendees using SVG templates. Many events use a variety of badges for different attendee types and access levels. This software allows you to generate personalized badges of a variety of types.  Badges can be printed from attendee data loaded from a CSV file or by polling a web service you provide.  The badges are defined by types. Each type can have a separate SVG template file to define it's appearance. The SVG is dynamically modified based on the loaded badge data. For example the SVG template might include a text element named "FIRST_NAME" which is replaced with the person's first name. In addition, the software can generate a QR Code and a Barcode based on user data which can be positioned in the SVG template.

The software can either generate PNG images for each badge or print the badges to a printer. At the moment the software assumes that you are generating 4"x6" badges and has only been tested using a HiTi P720L photo printer. It can be easily customized for other badge sizes and printers. Currently that customization will require changing constants in the code, but hopefully soon I will add a settings dialog.

If you intend to use this software, please send me a note and tell me about your project.  I am especially interested in what size badges and what printers will be used.

Usage
-----

The software will look for a folder called "badgedata" in the current directory.  It expects to find the definition of badge types, SVG templates and pictures here.

You will need to start by defining at least one SVG template.  For now just create a 4.1" x 6.1" page with a single text field.  Set the ID of this text field to TYPE.  The text can say whatever you want in the template as it will be replaced by the software.

Name the template badgedata/badge.svg.

Create a file called badgedata/attendee.utf8.properties which contains:

```
template=badge.svg
```


