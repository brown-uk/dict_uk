var err = initInstall("Ukrainian Dictionary", "uk-ua@dictionaries.addons.mozilla.org", "@VERSION@");
if (err != SUCCESS)
    cancelInstall();

var fProgram = getFolder("Program");
err = addDirectory("", "uk-ua@dictionaries.addons.mozilla.org",
		   "dictionaries", fProgram, "dictionaries", true);
if (err != SUCCESS)
    cancelInstall();

performInstall();
