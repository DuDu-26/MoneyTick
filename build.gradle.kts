// Plugins are declared per module (no root classpath) so :core can build on machines
// without access to Google's Maven repository (use -PcoreOnly=true there), while :app
// loads the Android and Kotlin plugins together in one classloader.
