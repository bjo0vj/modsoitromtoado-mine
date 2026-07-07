const tokenAuth = (req, res, next) => {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.split(' ')[1];

    if (token === 'fubabeo-admin-token-3667') {
        return next();
    }

    res.status(401).json({ success: false, message: 'Unauthorized' });
};

module.exports = tokenAuth;
