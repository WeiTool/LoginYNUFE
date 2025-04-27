use crate::Result;
use quick_error::quick_error;
use std::{
    net::{TcpStream, ToSocketAddrs},
    time::{Duration, SystemTime},
};

quick_error! {
    #[derive(Debug)]
    pub enum UtilError {
        AddrResolveError
    }
}

pub fn tcp_ping(addr: &str) -> Result<u16> {
    let addr = addr.to_socket_addrs()?.next();
    if addr.is_none() {
        return Err(Box::new(UtilError::AddrResolveError));
    }
    let timeout = Duration::from_secs(3);
    let start_time = SystemTime::now();
    let stream = TcpStream::connect_timeout(&addr.unwrap(), timeout)?;
    stream.peer_addr()?;
    let d = SystemTime::now().duration_since(start_time)?;
    Ok(d.as_millis() as u16)
}

#[test]
fn test_tcp_ping() {
    let p = tcp_ping("baidu.com:80");
    println!("{:?}", p);
}