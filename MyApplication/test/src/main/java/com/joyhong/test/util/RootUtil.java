package com.joyhong.test.util;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RootUtil {

    public boolean isRoot(RootResult rootResult) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                Log.e("GGGGG","ROOT SUCCESS 22");
                rootResult.success();
                return true;
            } else {
                Log.e("GGGGG","ROOT Fail");
                rootResult.fail();
                return false;
            }
        } catch (Exception e) {
            rootResult.fail();
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // 执行命令并且输出结果
    public static String execRootCmd(String cmd) {
        String result = "";
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());

            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            String line = null;
            while ((line = dis.readLine()) != null) {
                Log.d("result", line);
                result += line;
            }
            int exitValue = p.waitFor();
            Log.e("GGGGG","exitValue "+exitValue);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    interface RootResult {
        void success();

        void fail();
    }
}
