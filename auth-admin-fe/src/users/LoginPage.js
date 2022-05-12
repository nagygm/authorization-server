import React, { useState, useEffect } from 'react';
import { useLogin } from 'react-admin';

import { withStyles, createStyles } from '@material-ui/core/styles';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';

const styles = ({ spacing }) =>
    createStyles({

        button: {
            width: '100%',
            position: 'absolute',
            margin: 'auto',
            left: '50%',
            right: 0,
            top: '20%',
            bottom: 0,
        },
        icon: {
            marginRight: spacing.unit,
        },
    });

const LoginForm = () => {
    const login = useLogin()
    useEffect(() => {
        console.log("login form rendered")
        const location = window.location.href;
        const url = new URL(window.location.href);
        const { searchParams } = url ;
        const code = searchParams.get('code');
        const state = searchParams.get('state');

        if (code && state) {
            console.log("oauth callback received")
            login({ code: code, state: state });
        }
    });

    const handleLogin = () => {
        login();
    };

    return (
        <div>
            <CardActions>
                <Button className={'button'}
                    variant="contained"
                    type="submit"
                    color="primary"
                    onClick={handleLogin}
                >
                    Login
                </Button>
            </CardActions>
        </div>
    );
}

export default LoginForm;
