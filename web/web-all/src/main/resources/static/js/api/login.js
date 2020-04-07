var login = {

    api_name : '/api/user/passport',

  login(userInfo) {
    return request({
      url: this.api_name + '/login', // /api/user/passport/login
      method: 'post',
      data: userInfo
    })
  },

  logout() {
    return request({
      url: this.api_name + '/logout', // /api/user/passport/logout
      method: 'get'
    })
  }
}
