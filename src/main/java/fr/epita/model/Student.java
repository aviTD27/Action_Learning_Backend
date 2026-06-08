package fr.epita.model;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private int age;

    @Column
    private String address;

    // Default constructor (required for JPA/Jackson)
    public Student() {
    }

    // Full constructor
    public Student(String name, String surname, String email, int age, String address) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.age = age;
        this.address = address;
    }

    // Builder constructor
    public Student(Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.age = builder.age;
        this.address = builder.address;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setEmail( String email){
        this.email = email;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Student{" +
                "Name='" + name + '\'' +
                ", Surname='" + surname + '\'' +
                ", Email='" + email + '\'' +
                ", age=" + age +
                ", address='" + address + '\'' +
                '}';
    }


    public static class Builder {
            private String name;
            private String surname;
            private String email ;
            private int age;
            private String address ;

            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            public Builder setSurname(String surname) {
                this.surname = surname;
                return this;
            }
            public Builder setEmail( String email){
                this.email = email ;
                return this ;
            }

            public Builder setAge(int age) {
                this.age = age;
                return this;
            }

            public Builder setAddress(String address) {
                this.address = address;
                return this;
            }

            public Student build() {
                return new Student(this);
            }
        }

}
