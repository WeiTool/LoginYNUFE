use jni::{
    JNIEnv,
    objects::{JClass, JString, JObject, JValue, GlobalRef},
    sys::jboolean,
};
use crate::srun_login;

#[no_mangle]
pub extern "system" fn Java_com_srun_campuslogin_core_LoginBridge_nativeLogin(
    mut env: JNIEnv,
    _: JClass,
    username: JString,
    password: JString,
    detect_ip: jboolean,
    callback: JObject,
) {
    // 显式声明 GlobalRef 类型
    let global_callback: GlobalRef = match env.new_global_ref(callback) {
        Ok(r) => r,
        Err(e) => {
            env.throw(format!("创建全局引用失败: {}", e)).ok();
            return;
        }
    };

    // 后续代码保持不变...
    let jvm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(e) => {
            env.throw(format!("获取JavaVM失败: {}", e)).ok();
            return;
        }
    };

    let username = match env.get_string(&username) {
        Ok(jstr) => {
            match jstr.to_str() {
                Ok(s) => s.to_owned(),
                Err(e) => {
                    env.throw(format!("用户名转换失败: {}", e)).ok();
                    return;
                }
            }
        },
        Err(e) => {
            env.throw(format!("获取用户名失败: {}", e)).ok();
            return;
        }
    };

    let password = match env.get_string(&password) {
        Ok(jstr) => {
            match jstr.to_str() {
                Ok(s) => s.to_owned(),
                Err(e) => {
                    env.throw(format!("密码转换失败: {}", e)).ok();
                    return;
                }
            }
        },
        Err(e) => {
            env.throw(format!("获取密码失败: {}", e)).ok();
            return;
        }
    };

    std::thread::spawn(move || {
        let mut env = match jvm.attach_current_thread() {
            Ok(e) => e,
            Err(e) => {
                println!("线程附加失败: {}", e);
                return;
            }
        };

        match srun_login(&username, &password, detect_ip != 0) {
            Ok(_) => {
                if let Err(e) = env.call_method(
                    &global_callback,
                    "onSuccess",
                    "()V",
                    &[]
                ) {
                    println!("回调onSuccess失败: {}", e);
                }
            },
            Err(e) => {
                let error_msg = match env.new_string(format!("{}", e)) {
                    Ok(s) => s,
                    Err(e) => {
                        println!("创建错误消息失败: {}", e);
                        return;
                    }
                };

                if let Err(e) = env.call_method(
                    &global_callback,
                    "onFailure",
                    "(Ljava/lang/String;)V",
                    &[JValue::from(&error_msg)]
                ) {
                    println!("回调onFailure失败: {}", e);
                }
            }
        }
    });
}