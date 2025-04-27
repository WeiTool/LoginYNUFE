pub use srun::*;
pub use user::User;
pub use xencode::param_i;

#[cfg(feature = "ureq")]
mod http_client;
pub mod srun;
mod user;
mod utils;
mod xencode;

pub type Result<T> = std::result::Result<T, Box<dyn std::error::Error>>;

pub struct LoginOptions {
    pub detect_ip: bool,
    pub strict_bind: bool,
    pub test_before_login: bool,
}

impl Default for LoginOptions {
    fn default() -> Self {
        Self {
            detect_ip: false,
            strict_bind: false,
            test_before_login: false,
        }
    }
}

pub fn login_with_options(
    username: &str,
    password: &str,
    options: &LoginOptions,
) -> Result<()> {
    let server = "http://172.16.130.31/";

    let user = User {
        username: username.to_owned(),
        password: password.to_owned(),
        ip: None,
    };

    SrunClient::new_from_user(server, user)
        .set_detect_ip(options.detect_ip)
        .set_strict_bind(options.strict_bind)
        .set_test_before_login(options.test_before_login)
        .login()
}

#[cfg(target_os = "android")]
mod android;

// 导出核心功能
pub fn srun_login(username: &str, password: &str, detect_ip: bool) -> Result<()> {
    let server = "http://172.16.130.31/";

    let user = User {
        username: username.to_owned(),
        password: password.to_owned(),
        ip: None,
    };

    SrunClient::new_from_user(server, user)
        .set_detect_ip(detect_ip)
        .login()
}

