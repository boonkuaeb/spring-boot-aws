package com.bk.springbootaws.controller;

import com.bk.springbootaws.exception.CustomerNotFoundException;
import com.bk.springbootaws.exception.InvalidCustomerRequestException;
import com.bk.springbootaws.model.Address;
import com.bk.springbootaws.model.Customer;
import com.bk.springbootaws.model.CustomerImage;
import com.bk.springbootaws.repository.CustomerRepository;
import com.bk.springbootaws.service.FileArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Customer Controller exposes a series of RESTful endpoints
 */
@RestController
public class CustomerController {

	@Autowired
	private CustomerRepository customerRepository;
	
	@Autowired
	private FileArchiveService fileArchiveService;
	 
	
	@RequestMapping(value = "/customers", method = RequestMethod.POST)
    public @ResponseBody
    Customer createCustomer(
            @RequestParam(value="firstName", required=true) String firstName,
            @RequestParam(value="lastName", required=true) String lastName,
            @RequestParam(value="dateOfBirth", required=true) @DateTimeFormat(pattern="yyyy-MM-dd") Date dateOfBirth,
            @RequestParam(value="street", required=true) String street,
            @RequestParam(value="town", required=true) String town,
            @RequestParam(value="county", required=true) String county,
            @RequestParam(value="postcode", required=true) String postcode,
            @RequestParam(value="image", required=true) MultipartFile image) {
        
        	CustomerImage customerImage = fileArchiveService.saveFileToS3(image);
        	Customer customer = new Customer(firstName, lastName, dateOfBirth, customerImage,
        										new Address(street, town, county, postcode));
        	
        	customerRepository.save(customer);
            return customer;               
    }
	
	/**
	 * Get customer using id. Returns HTTP 404 if customer not found
	 * 
	 * @param customerId
	 * @return retrieved customer
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.GET)
	public Customer getCustomer(@PathVariable("customerId") Long customerId) {
		
		/* validate customer Id parameter */
		if (null==customerId) {
			throw new InvalidCustomerRequestException();
		}
		
		Optional<Customer> customer = customerRepository.findById(customerId);
		
		if(!customer.isPresent()){
			throw new CustomerNotFoundException();
		}
		
		return customer.get();
	}
	
	/**
	 * Gets all customers.
	 *
	 * @return the customers
	 */
	@RequestMapping(value = "/customers", method = RequestMethod.GET)
	public List<Customer> getCustomers() {
		
		return (List<Customer>) customerRepository.findAll();
	}
	
	/**
	 * Deletes the customer with given customer id if it exists and returns HTTP204.
	 *
	 * @param customerId the customer id
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.DELETE)
	public void removeCustomer(@PathVariable("customerId") Long customerId, HttpServletResponse httpResponse) {

		if(customerRepository.existsById(customerId)){
			Optional<Customer> customer = customerRepository.findById(customerId);
			if (customer.isPresent()) {
				fileArchiveService.deleteImageFromS3(customer.get().getCustomerImage());
				customerRepository.delete(customer.get());
			}
		}
		
		httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
	}

}