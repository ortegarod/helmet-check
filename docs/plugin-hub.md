plugin-hub Discord

This repository contains markers for RuneLite plugins that are not supported by the RuneLite Developers. The plugins are provided "as is"; we make no guarantees about any plugin in this repo.

Setting up the development environment

We recommend IntelliJ Idea Community Edition as well as Java 11. You can either have IntelliJ install Java (select Eclipse Temurin) or download it from https://adoptium.net/temurin/releases/. You must also have a GitHub account.

Contribute to existing plugins

We recommend contributing to existing plugins if the author(s) are accepting contributions, and the feature you want to add fits well into the plugin, to avoid fragmentation of plugin ecosystem. Reducing plugin fragmentation helps users discover features more easily, and helps us review changes in a more timely manner.

You may contribute to existing plugins by selecting the plugin from https://runelite.net/plugin-hub, navigating to the plugin's GitHub repository by following the "Report an issue" link, and then following the "Create new plugins" section below from step 3.

Creating new plugins

Generate your own repository from the plugin template link (you must be signed into GitHub first). Alternatively, you may use the create_new_plugin.py script provided in this repository to generate a new plugin project.

Name your repository something appropriate, in my case I will name it helmet-check with the description You should always wear a helmet. Make sure that your repository is set to public.

In the top right, you will see a Clone or download-button. Click on it and copy the link.

Open IntelliJ and choose Get from Version Control. Paste the link you just copied in the URL field and where you want to save it in the second field.

In order to make sure everything works correctly, try to start the client with your external plugin enabled by running the provided test. If you don't have a run configuration yet for the test, attempt to run it by clicking Run test. This will create a run configuration and fail to run due to asserts being disabled. Add -ea to your VM options in the run configuration to enable assertions, which can be found under Run/Debug Configurations under Modify options, Add VM options, and then adding -ea into the input field which appears.

The client should now launch with your plugin enabled. If you have a Jagex account, you will be unable to login without first following this guide.

run-test

Use the refactor tool to rename the package to what you want your plugin to be. Rightclick the package in the sidebar and choose Refactor > Rename. I choose to rename it to com.helmetcheck.

Use the same tool, Refactor > Rename, to rename ExamplePlugin, ExampleConfig and ExamplePluginTest to HelmetCheckPlugin etc.

Go to your plugin file and set its name in the PluginDescriptor, this can have spaces.

Open the runelite-plugin.properties file and add info to each row.

displayName=Helmet check
author=dekvall
description=Alerts you when you have nothing equipped in your head slot
tags=hint,gear,head
plugins=com.helmetcheck.HelmetCheckPlugin
tags will make it easier to find your plugin when searching for related words. If you want to add multiple plugin files, the plugins field allows for comma separated values, but this is not usually needed.

Optionally, you can add an icon to be displayed alongside with your plugin. Place a file with the name icon.png no larger than 48x72 px at the root of the repository.

Write a nice README so your users can see the features of your plugin.

When you have your plugin working. Commit your changes and push them to your repository.

Licensing your repository

Go to your repository on GitHub and select Add file (next to the green Code button), and choose Create new file from the drop-down.
In the file name field type LICENSE and click the Choose a license template button that will appear.
Select BSD 2-Clause "Simplified" License from the list to the left. Fill in your details and press Review and submit.
Commit your changes by clicking Commit changes at the bottom of the page. Make sure you check the button to directly commit to the master branch.
Submitting a plugin

Fork the plugin-hub repository.
Create a new branch for your plugin.
Create a new file in the plugin-hub/plugins directory with the fields:
repository=
commit=
To get the repository url, click the Clone or download-button choose Use HTTPS. Paste the url in in the repository= field.

To get the commit hash, go to your plugin repository on GitHub and click on commits. Choose the latest one and copy the full 40-character hash. It can be seen in the top right after selecting a commit. Paste this into the commit= field of the file. Your file should now look something like this:

repository=https://github.com/dekvall/helmet-check.git
commit=9db374fc205c5aae1f99bd5fd127266076f40ec8
This is the only change you need to make, so commit your changes and push them to your fork. Then go back to the plugin-hub and click New pull request in the upper left. Choose Compare across forks and select your fork and branch as head and compare.

Write a short description of what your plugin does and then create your pull request.

Check the result of your PR's CI workflow, next to .github/workflows/build.yml / build (pull_request) will be either a ✔️ or an ❌. With a ✔️ all is good, however if it has an ❌, click Details to check the build log for details of the failure. Along with the build workflow there also may be an ❌ next to RuneLite Plugin Hub Checks, you will only need to worry about this if it says Changes are needed., in that case you should also read over those requested changes. Once you've read over the build error and requested changes, make the required changes, and push another commit to update the PR with the new commit= hash.
Don't worry about how many times it takes you to resolve build errors; we prefer all changes be kept in a single pull request to avoid spamming notifications with further newly-opened PRs.

Be patient and wait for your plugin to be reviewed and merged.

Updating a plugin

To update a plugin, simply update the manifest with the most recent commit hash.

It is recommended to open a pull request from a separate branch. You can run the following commands from your plugin-hub repository directory to set up a branch:

$ git remote add upstream https://github.com/runelite/plugin-hub.git  # Only necessary if you have not set it before
$ git fetch upstream && git checkout -B <your-plugin-name> upstream/master
Once your changes have been merged, you can delete the branch. The next time you would like to update your plugin, you can create the branch again.

Reviewing

We will review your plugin to ensure it isn't malicious, doesn't break Jagex's rules, or isn't one of our previously Rejected/Rolledback features.
If it is difficult for us to ensure the plugin isn't against the rules we will not merge it.

Plugin resources

Resources may be included with plugins, which are non-code and are bundled and distributed with the plugin, such as images and sounds. You may do this by placing them in src/main/resources. Plugins on the pluginhub are distributed in .jar form and the jars placed into the classpath. The plugin is not unpacked on disk, and you can not assume that it is. This means that using https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResource-java.lang.String- will return a jar-URL when the plugin is deployed to the pluginhub, but in your IDE will be a file-URL. This almost certainly makes it behave differently from how you expect it to, and isn't what you want. Instead, prefer using https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getResourceAsStream-java.lang.String-.

Third party dependencies

We require any dependencies that are not a transitive dependency of runelite-client to be have their cryptographic hash verified during the build to prevent supply chain attacks and ensure build reproducability. To do this we rely on Gradle's dependency verification. To add a new dependency, add it to the thirdParty configuration in package/verification-template/build.gradle, then run ../gradlew --write-verification-metadata sha256 to update the metadata file. A maintainer must then verify the dependencies manually before your pull request will be merged. This process generally adds significantly to the amount of time it takes for a plugin submission or update to be reviewed, so we recommend avoiding adding any new dependencies unless absolutely necessary.

My client version is outdated

If your client version is outdated or your plugin suddenly stopped working after RuneLite has been updated, make sure that your runeLiteVersion is set to 'latest.release' in build.gradle. If this is set correctly, refresh the Gradle dependencies by doing the following:

Open the Gradle tool window.
Right-click on the project's name. This will contain the Gradle icon (elephant).
Choose Refresh Gradle Dependencies. If your issue is not resolved, try reloading all Gradle projects. This option is located in the toolbar in the Gradle tool window. Additionally, try invalidating caches.