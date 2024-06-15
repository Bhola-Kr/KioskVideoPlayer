
package com.koisk.videokiosk.utils;

import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.LocalData;

import java.io.File;

public class Method {

    public static void load_Directory_Files(File directory){
        File[] fileList = directory.listFiles();
        if(fileList != null && fileList.length > 0){
            for (int i=0; i<fileList.length; i++){
                if(fileList[i].isDirectory()){
                    load_Directory_Files(fileList[i]);
                }
                else {
                    String name = fileList[i].getName().toLowerCase();
                    for (String extension: Constant.videoExtensions){
                        //check the type of file
                        if(name.endsWith(extension)){
                            LocalData.allMediaList.add(fileList[i]);
                            //when we found file
                            break;
                        }
                    }
                }
            }
        }
    }

}
