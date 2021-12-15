import * as React from "react";
import { List, Datagrid, Edit, Create, SimpleForm, SelectInput, BooleanField, BooleanInput, TextField, EditButton, TextInput, DateInput } from 'react-admin';

export const UserList = (props) => (
    <List {...props}>
        <Datagrid>
            <TextField source="id" />
            <TextField source="firstName" />
            <TextField source="lastName" />
            <TextField source="username" />
            <TextField source="email" />
            <BooleanField source="enabled"/>
            <EditButton basePath="/users" />
        </Datagrid>
    </List>
);

const UserTitle = ({ record }) => {
    return <span>User {record ? `${record.firstName} ${record.lastName}` : ''}</span>;
};

export const UserEdit = (props) => (
    <Edit title={<UserTitle />} {...props}>
        <SimpleForm>
            <TextInput disabled source="id" />
            <TextInput source="firstName" />
            <TextInput source="lastName" />
            <TextInput source="username" />
            <TextInput disabled source="email" />
            <BooleanInput source='enabled' />
        </SimpleForm>
    </Edit>
);

export const UserCreate = (props) => (
    <Create title="Create a User" {...props}>
        <SimpleForm>
            <TextInput source="firstName" />
            <TextInput source="lastName" />
            <TextInput source="username" />
            <TextInput source="email" />
            <BooleanInput source='enabled' />
        </SimpleForm>
    </Create>
);
