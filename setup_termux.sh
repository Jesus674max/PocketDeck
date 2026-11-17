#!/data/data/com.termux/files/usr/bin/bash
# Ejecuta esto UNA VEZ en Termux (no dentro de Ubuntu).
pkg install -y tmux
mkdir -p ~/.termux ~/pocketmine
grep -q allow-external-apps ~/.termux/termux.properties 2>/dev/null || \
  echo "allow-external-apps = true" >> ~/.termux/termux.properties
cat > ~/pocketmine/start.sh <<'EOS'
#!/data/data/com.termux/files/usr/bin/bash
cd ~/pocketmine
./bin/php7/bin/php PocketMine-MP.phar
EOS
chmod +x ~/pocketmine/start.sh
termux-reload-settings
echo "Listo. Pon tu PHP en ~/pocketmine/bin/php7/bin/php y PocketMine-MP.phar en ~/pocketmine/"
