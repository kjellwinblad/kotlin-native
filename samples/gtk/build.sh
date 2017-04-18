#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

CFLAGS_macbook="-I/opt/local/include/gtk-3.0 -I/opt/local/include/glib-2.0"
CFLAGS_linux=-I/usr/include
LINKER_ARGS_macbook="-L/opt/local/lib -lglib-2.0 -lgdk-3.0 -lgtk-3 -lgio-2.0 -lgobject-2.0"
LINKER_ARGS_linux="-L/usr/lib/x86_64-linux-gnu -lgtk3"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS="${!var}"
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

cinterop -J-Xmx8g -copt -I/opt/local/include/atk-1.0 -copt -I/opt/local/include/gdk-pixbuf-2.0 -copt -I/opt/local/include/cairo -copt -I/opt/local/include/pango-1.0 -copt -I/opt/local/lib/glib-2.0/include -copt -I/opt/local/include/gtk-3.0 -copt -I/opt/local/include/glib-2.0 -def $DIR/gtk3.def -target $TARGET -o gtk3.bc || exit 1
#konanc -target $TARGET src -library gtk3.bc -linkerArgs "$LINKER_ARS" -o Gtk3Demo.kexe || exit 1
konanc -J-Xmx10g src gtk3.bc-build/kotlin/gtk3/gtk3.kt  -nativelibrary gtk3.bc-build/natives/cstubs.bc -o Gtk.kexe -linkerArgs "-L/opt/local/lib -lglib-2.0 -lgdk-3.0 -lgtk-3 -lgio-2.0 -lgobject-2.0"


