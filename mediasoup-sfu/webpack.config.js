const path = require('path');

module.exports = {
    mode: 'development',  // 모드를 'development'로 설정
    entry: './public/script.js',  // 시작 파일
    output: {
        filename: 'bundle.js',  // 번들된 파일
        path: path.resolve(__dirname, 'public'),
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-env'],
                    },
                },
            },
        ],
    },
    resolve: {
        alias: {
            'mediasoup-client': path.resolve(__dirname, 'node_modules/mediasoup-client/lib/index.js'), // 정확한 경로 설정
        },
    },
    devServer: {
        static: path.join(__dirname, 'public'),
        port: 3001,
    },
};
