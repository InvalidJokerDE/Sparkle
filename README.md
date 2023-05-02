# ![Sparkle Banner](https://user-images.githubusercontent.com/28064149/193403722-69ddfc53-95b7-4123-b974-308d9360cdb3.jpg)

<br>

# 👋 Welcome to Sparkle!
Sparkle is a full-fledged framework, developed for the development of Plugins on the Paper Minecraft-Server platform, using the Kotlin programming language.

This framework combines simple and fast tools with under-the-hood rocket science, to enable a new feature-rich architecture, for plugins, to build on.

# 📑 Version
Many plugins do everything, to be supported on as many versions of Minecraft as possible. If you want to use this framework with this goal in mind, this may not be the right thing for you. This framework is specialized to make the most out of current and coming technology. This is the reason, why we always build on-top the newest (or current newest stable) version, available right now.

# 🎯 Goal
Our goal is to provide powerful tools and a great foundation, which helps to easily develop plugins, without blocking the way to get deeper.

# 💻 Set up your Project
Our current platform of trust for our builds is 'JitPack'. This allows us to easily provide you with the current version of Sparkle, without all the hustle of GitHub's own “solution”.

Firstly, add JitPack as a maven repository if it is not already added!
```kotlin
maven("https://jitpack.io")
```

Subsequently, the only thing missing is sparkle itself, add this to your dependencies:
```kotlin
implementation("com.github.TheFruxz:sparkle:$sparkleVersion")
```

And done.

# 🌋 Running Sparkle
Currently, due to some features and actively running code, it is required, that the Sparkle plugin is running on the server.
The running plugin not only allows automatic services, but also loading the required dependencies.
Things like the Kotlin-Libraries and Ascend + Stacked are all loaded via the Paper-Plugin dependency feature, so the Sparkle-Plugin file is lightweight and your Paper-Server provides you with the stuff, you need.

# 🧑‍💻 Contribution

Of course, you can also participate in Sparkle and contribute to the development. However, please follow all community and general guidelines of GitHub and the repositories. You also have to respect the licenses set in this repository as well as in other repositories.

If you require any further assistance, suggestions or other items you would like to contribute to Sparkle or just discuss, take a look at the Discussions' section of this repository. There you will find the respective areas where you can create your own questions or join in discussions on other things.

###### We build & use Sparkle on Java 17 - [Eclipse Temurin](https://adoptium.net/).
###### Also build & run Sparkle with [Eclipse Temurin](https://adoptium.net/) to get the best possible experience!

[![Open Source](https://forthebadge.com/images/badges/open-source.svg)](https://github.com/TheFruxz/Sparkle/blob/main/LICENSE)
[![Built by developers](https://forthebadge.com/images/badges/built-by-developers.svg)](https://github.com/TheFruxz/Sparkle/graphs/contributors)
[![Written in Kotlin](https://forthebadge.com/images/badges/makes-people-smile.svg)](https://github.com/JetBrains/kotlin)
