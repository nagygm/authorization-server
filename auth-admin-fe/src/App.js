import * as React from "react";
import { Admin, Resource, ListGuesser, CustomRoutes} from 'react-admin';
// import jsonServerProvider from 'ra-data-json-server';
import {SpringDataProvider} from "./components/dataprovider";
import UserIcon from '@material-ui/icons/People';
import DeviceIcon from '@material-ui/icons/Devices'
import AppsIcon from '@material-ui/icons/Apps'
import {UserCreate, UserEdit, UserList} from "./users/users";
import {ClientCreate, ClientEdit, ClientList} from "./clients/clients";
import {ResourceCreate, ResourceEdit, ResourceList} from "./resources/resources";
import {authProvider} from './components/authprovider';
import LoginForm from "./users/LoginPage";


const App = () => (
    <Admin dataProvider={SpringDataProvider}
           authProvider={authProvider}
           loginPage={LoginForm}>
        <Resource name="users" list={UserList} edit={UserEdit} create={UserCreate} icon={UserIcon}/>
        <Resource name="clients" icon={DeviceIcon} list={ClientList} create={ClientCreate} edit={ClientEdit}/>
        <Resource name="resources" icon={AppsIcon} list={ResourceList} create={ResourceCreate} edit={ResourceEdit}/>
    </Admin>
);
export default App;
