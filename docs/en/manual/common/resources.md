# Resources

The fonts and the images of a document are in a **resources folder**, by default `resources/` in the directory from
which the conversion is run:

```
resources/
    fonts/      .ttf and .otf fonts
    images/     .png, .jpg and .svg images
```

Inside each one, subfolders are free. The folder can be changed when converting ([Using from Java](java-usage.md)).

## Fonts

- Only the `.ttf` and `.otf` fonts of `fonts/` are used: not those of the system. They are embedded in the PDF, so it
  looks the same on any computer. Their license has to allow it, as the SIL Open Font License does.
- Each file is recognized by the family, the weight and the style it declares inside, not by its name.
  `Font: IBM Plex Sans` with `Bold: true` looks for the file of that family with weight 700, and with `Weight: 300`,
  the one with weight 300.
- If a text asks for a variant that has no file (a weight the family does not have, or bold with italic), it is an
  error: it is not made up and the closest one is not used.
- The repository includes IBM Plex Sans, Serif and Mono, with the four variants (regular, bold, italic and bold italic),
  and IBM Plex Sans also in weights 300, 500 and 600.

A new font is added by copying its static files into `fonts/` (variable fonts are not valid) and is used with its
family in `Font`. If the name is misspelled, the error lists the available families. If the font is distributed as
`.ttf` and as `.otf`, only one of the two is copied (both together are an error), preferably the `.ttf`: many `.otf`
files are embedded whole and make the PDF larger ([details](../../reference/common/resources.md#fonts)).

## Images

`Image: logos/acme.png` is the file `resources/images/logos/acme.png` ([Image](../components/image.md)). Paths always go
downward from `images/`: absolute paths, `..` and links that leave the folder are not accepted.

The files are checked before layout: a missing font or image is an error with the line of the document.

More detail in the [reference](../../reference/common/resources.md).
