use serde::Deserialize;

#[derive(Debug, Default, Deserialize, Clone)]
pub struct User {
    pub username: String,
    pub password: String,
    pub ip: Option<String>,
}

