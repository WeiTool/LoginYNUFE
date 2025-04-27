use std::{net::Ipv4Addr};

fn main() {
    let auth_server_ip = "172.16.130.31";  // 硬编码 IP
    println!("ENV AUTH_SERVER_IP = {auth_server_ip}");
    auth_server_ip
        .parse::<Ipv4Addr>()
        .expect("AUTH_SERVER_IP invalid");
}
