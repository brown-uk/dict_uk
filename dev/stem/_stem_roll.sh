#!/bin/sh

if [ "$1" == "-c" ]; then 
  cp dict_corp_vis.txt.roots dict_corp_vis.txt.roots.prev
  cp dict_corp_vis.txt.roots1 dict_corp_vis.txt.roots1.prev
else 
  mv dict_corp_vis.txt.roots dict_corp_vis.txt.roots.prev
  mv dict_corp_vis.txt.roots1 dict_corp_vis.txt.roots1.prev
fi

