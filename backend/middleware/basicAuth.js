const basicAuth = (req, res, next) => {
    const b64auth = (req.headers.authorization || '').split(' ')[1] || '';
    const [login, password] = Buffer.from(b64auth, 'base64').toString().split(':');

    if (login === 'zuybell' && password === 'tdf0833') {
        return next();
    }

    res.set('WWW-Authenticate', 'Basic realm="Dashboard"');
    res.status(401).send('Authentication required.');
};

module.exports = basicAuth;
