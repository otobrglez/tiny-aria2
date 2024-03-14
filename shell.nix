# The Nix Shell support was added by Oto Brglez.
# If you have additional questions or need support, please reach out.
# - @otobrglez

{ pkgs ? import <nixpkgs> { } }:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = jdk21_headless; };
in


mkShell {
  name = "tiny-aria2";
  buildInputs = [
    jdk21_headless
    sbt
  ];

  NIX_ENFORCE_PURITY = 0;
  NIX_SHELL_PRESERVE_PROMPT = 1;
  NIXPKGS_ALLOW_UNFREE = 1;
  
  shellHook = ''
    export JAVA_HOME="${jdk21_headless}"
    echo JAVA_HOME=$JAVA_HOME
  '';
}
