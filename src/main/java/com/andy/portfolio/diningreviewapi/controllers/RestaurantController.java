package com.andy.portfolio.diningreviewapi.controllers;

import com.andy.portfolio.diningreviewapi.model.Restaurant;
import com.andy.portfolio.diningreviewapi.repositories.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {
    final private RestaurantRepository restaurantRepository;

    final Pattern zipCodePattern = Pattern.compile("[a-z0-9- ]{0,10}[a-z0-9]+");
    public RestaurantController(final RestaurantRepository restaurantRepository){this.restaurantRepository = restaurantRepository;}

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create-restaurant")
    public Restaurant createRestaurant(@RequestBody Restaurant restaurant) throws Exception {
        if(!validateRestaurant(restaurant)) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "The provided zipcode is of an invalid format or the restaurant hasn't been found.");
        }
        this.restaurantRepository.save(restaurant);
        return restaurant;
    }

    @PutMapping("/update-restaurant/{id}")
    public Restaurant modifyRestaurant(@RequestBody Restaurant restaurant, @PathVariable Long id) throws Exception {
        if(!validateRestaurant(restaurant)) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "The provided zipcode is of an invalid format or the restaurant hasn't been found.");
        }
        var restaurantToUpdateOptional = Optional.ofNullable(this.restaurantRepository.findById(id));
        if (restaurantToUpdateOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The provided id doesn't exist in the database.");
        }
        var restaurantToUpdate = restaurantToUpdateOptional.get();

        //Update fields only if the parameter is given.
        //Also, Updating name and zipcode will only update if the field in the database is different from the provided ones
        //The overall and average score(s) shouldn't be updated and always be calculated
        if (!Objects.equals(restaurantToUpdate.getName(), restaurant.getName())){restaurantToUpdate.setName(restaurant.getName());}
        if (!Objects.equals(restaurantToUpdate.getZipCode(), restaurant.getZipCode())){restaurantToUpdate.setZipCode(restaurant.getZipCode());}
        if (restaurant.getCountry() != null) {restaurantToUpdate.setCountry(restaurant.getCountry());}

        this.restaurantRepository.save(restaurantToUpdate);
        return restaurantToUpdate;
    }

    @GetMapping("/")
    public Iterable<Restaurant> getAllRestaurants(){
        return this.restaurantRepository.findAll();
    }

    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("/country/{country}/{ascendingOrder}")
    public Iterable<Restaurant> getAllRestaurantsFromCountry(@PathVariable String country, @PathVariable Boolean ascendingOrder){
        if(ascendingOrder) {
            return this.restaurantRepository.findByCountryOrderByNameAsc(country);
        } else {
            return this.restaurantRepository.findByCountryOrderByNameDesc(country);
        }
    }

    @GetMapping("/city/{city}")
    public Iterable<Restaurant> getAllRestaurantsFromCityByCityName(@PathVariable String city, Boolean ascendingOrder){
        if(ascendingOrder) {
            return this.restaurantRepository.findByCityOrderByNameAsc(city);
        } else {
            return this.restaurantRepository.findByCityOrderByNameDesc(city);
        }
    }

    @GetMapping("/city/{zipcode}")
    public Iterable<Restaurant> getAllRestaurantsFromCityByZipCode(@PathVariable String zipCode, Boolean ascendingOrder) throws Exception {
        validateZipCode(zipCode);
        if(ascendingOrder) {
            return this.restaurantRepository.findByZipCodeOrderByNameAsc(zipCode);
        } else {
            return this.restaurantRepository.findByZipCodeOrderByNameDesc(zipCode);
        }
    }

    @GetMapping("/scores")
    public Iterable<Restaurant> getAllRestaurantsWithScoresByZipcode(String zipCode) throws Exception {
        validateZipCode(zipCode);
        return this.restaurantRepository.findByZipCodeAndAverageScoreEggNotNullOrAverageScoreDairyNotNullOrAverageScorePeanutNotNullOrderByZipCodeDesc(zipCode);
    }


    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("/id/{id}")
    public Restaurant getRestaurant(@PathVariable Long id){
        if(!this.restaurantRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found in database.");
        }
        return this.restaurantRepository.findById(id);
    }

    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("/name/{name}")
    public Restaurant getRestaurant(@PathVariable String name){
        return this.restaurantRepository.findByName(name);
    }

    public Boolean validateRestaurant(Restaurant restaurant) throws Exception {
        validateZipCode(restaurant.getZipCode());
        if (this.restaurantRepository.existsByNameAndZipCode(restaurant.getName(), restaurant.getZipCode())) {
            throw new ResponseStatusException(HttpStatus.IM_USED, "The provided restaurant already exists in the database. Please enter a different one.");
        }
        return true;
    }

    public Boolean validateZipCode(String zipCode) throws Exception{
        Matcher zipCodeMatcher = zipCodePattern.matcher(zipCode);
        if (!zipCodeMatcher.matches()) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "The provided zipcode is of an invalid format.");
        }
        return true;
    }
}
